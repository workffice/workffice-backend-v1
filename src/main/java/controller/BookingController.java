package controller;

import booking.application.booking.BookingFinder;
import booking.application.booking.PaymentPreferenceCreator;
import booking.application.dto.booking.BookingError;
import booking.domain.booking.BookingId;
import booking.infrastructure.mercadopago.MercadoPagoPaymentNotification;
import booking.infrastructure.mercadopago.MercadoPagoPaymentResolver;
import controller.response.DataResponse;
import controller.response.PaginatedResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
@RequestMapping(value = "/api/bookings")
public class BookingController extends BaseController {

    ResponseEntity<DataResponse> notFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("BOOKING_NOT_FOUND", "There is no booking with specified id"));
    ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("BOOKING_FORBIDDEN", "You don't have access to this booking"));
    ResponseEntity<DataResponse> invalidId = ResponseEntity.badRequest().body(invalid(
            "INVALID_BOOKING_ID",
            "Booking id provided is not valid"
    ));

    @Autowired private BookingFinder              finder;
    @Autowired private PaymentPreferenceCreator   paymentPreferenceCreator;
    @Autowired private MercadoPagoPaymentResolver mercadoPagoPaymentResolver;

    @GetMapping("/{id}/")
    public ResponseEntity<?> getBooking(@PathVariable String id) {
        try {
            var bookingId = BookingId.fromString(id);
            return finder.find(bookingId)
                    .map(booking -> ResponseEntity.ok((DataResponse) entityResponse(booking)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(BookingError.BOOKING_NOT_FOUND), notFound),
                            Case($(BookingError.BOOKING_FORBIDDEN), forbidden)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getRenterBookings(
            Pageable pageable,
            @RequestParam(name = "renter_email") String renterEmail,
            @RequestParam(name = "current_bookings") boolean currentBookings
    ) {
        return finder.find(renterEmail, currentBookings, pageable)
                .map(pageBookings -> {
                    DataResponse response = new PaginatedResponse<>(
                            pageBookings.getContent(),
                            pageBookings.getSize(),
                            pageBookings.isLast(),
                            pageBookings.getTotalPages(),
                            pageBookings.getTotalPages() == 0 ? 0 : pageBookings.getNumber() + 1
                    );
                    return ResponseEntity.ok(response);
                })
                .getOrElseGet(error -> Match(error).of(
                        Case($(BookingError.BOOKING_FORBIDDEN), forbidden)
                ));
    }

    @PostMapping("/{id}/mp_preferences/")
    public ResponseEntity<?> createMercadoPagoPreference(@PathVariable String id) {
        try {
            var bookingId = BookingId.fromString(id);
            ResponseEntity<DataResponse> bookingAlreadyScheduled = ResponseEntity
                    .badRequest()
                    .body(invalid(
                            "BOOKING_IS_NOT_PENDING",
                            "You can't create a payment preference for a booking that is no longer pending"
                    ));
            ResponseEntity<DataResponse> mercadoPagoError = ResponseEntity
                    .badRequest()
                    .body(invalid(
                            "MERCADO_PAGO_ERROR",
                            "Something went wrong with mercado pago try again later"
                    ));
            return paymentPreferenceCreator
                    .create(bookingId)
                    .map(paymentPreference -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body((DataResponse) entityResponse(paymentPreference)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(BookingError.BOOKING_NOT_FOUND), notFound),
                            Case($(BookingError.BOOKING_FORBIDDEN), forbidden),
                            Case($(BookingError.BOOKING_IS_NOT_PENDING), bookingAlreadyScheduled),
                            Case($(BookingError.MERCADO_PAGO_ERROR), mercadoPagoError)
                    ));
        } catch (IllegalArgumentException e) {
            return invalidId;
        }
    }

    @PostMapping("/{bookingId}/mp_notifications/")
    public ResponseEntity<?> mpNotification(
            @PathVariable String bookingId,
            @RequestBody MercadoPagoPaymentNotification notification
    ) {
        LoggerFactory.getLogger(getClass()).info(format("BOOKING_ID: %s", bookingId));
        LoggerFactory.getLogger(getClass()).info(notification.toString());
        if (notification.getAction().equals("payment.created") || notification.getAction().equals("payment.updated"))
            mercadoPagoPaymentResolver.handleNotification(bookingId, notification);
        return ResponseEntity.ok().build();
    }
}
