package controller;

import booking.application.membership_acquisition.MembershipAcquisitionCreator;
import booking.application.membership_acquisition.MembershipAcquisitionFinder;
import booking.application.membership_acquisition.MembershipAcquisitionPaymentPreferenceCreator;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.infrastructure.mercadopago.MercadoPagoPaymentNotification;
import booking.infrastructure.mercadopago.MercadoPagoPaymentResolver;
import controller.response.DataResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_PENDING;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_NOT_FOUND;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.NO_AUTHENTICATED_USER;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
public class MembershipAcquisitionController extends BaseController {
    @Autowired
    MembershipAcquisitionCreator creator;
    @Autowired
    MembershipAcquisitionFinder finder;
    @Autowired
    MembershipAcquisitionPaymentPreferenceCreator preferenceCreator;
    @Autowired
    MercadoPagoPaymentResolver mercadoPagoPaymentResolver;

    ResponseEntity<DataResponse> membershipAcquisitionForbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("MEMBERSHIP_ACQUISITION_FORBIDDEN", "You don't have access to acquire a membership"));

    ResponseEntity<DataResponse> forbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden(
                    "MEMBERSHIP_ACQUISITION_NOT_FOUND",
                    "The membership acquisition requested is not yours"));

    @PostMapping("/api/memberships/{membershipId}/acquisitions/")
    public ResponseEntity<?> acquireMembership(@PathVariable String membershipId) {
        try {
            var membershipAcquisitionId = new MembershipAcquisitionId();
            var createdResponse = ResponseEntity.status(HttpStatus.CREATED)
                    .body((DataResponse) entityCreated(format("/api/membership_acquisitions/%s/",
                            membershipAcquisitionId)));
            ResponseEntity<DataResponse> membershipNotFound = ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(notFound("MEMBERSHIP_NOT_FOUND", "The membership requested does not exist"));
            return creator.create(membershipAcquisitionId, membershipId)
                    .map(v -> createdResponse)
                    .getOrElseGet(error -> Match(error).of(
                            Case($(NO_AUTHENTICATED_USER), membershipAcquisitionForbidden),
                            Case($(MEMBERSHIP_NOT_FOUND), membershipNotFound)
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(invalid("INVALID_MEMBERSHIP_ID", "The membership id provided is invalid"));
        }
    }

    @GetMapping("/api/membership_acquisitions/")
    public ResponseEntity<?> myMembershipAcquisitions() {
        return finder
                .find()
                .map(membershipAcquisitions -> ResponseEntity.ok((DataResponse) entityResponse(membershipAcquisitions)))
                .getOrElseGet(error -> Match(error).of(
                        Case($(MEMBERSHIP_ACQUISITION_FORBIDDEN), forbidden)
                ));
    }

    @PostMapping("/api/membership_acquisitions/{id}/mp_preferences/")
    public ResponseEntity<?> createPreference(@PathVariable String id) {
        try {
            var membershipAcquisitionId = MembershipAcquisitionId.fromString(id);
            ResponseEntity<DataResponse> notFound = ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(notFound(
                            "MEMBERSHIP_ACQUISITION_NOT_FOUND",
                            "The membership acquisition requested does not exist"));
            ResponseEntity<DataResponse> isNotPending = ResponseEntity
                    .badRequest()
                    .body(invalid(
                            "MEMBERSHIP_ACQUISITION_NOT_PENDING",
                            "The membership acquisition requested is not pending"));
            return preferenceCreator.create(membershipAcquisitionId)
                    .map(paymentPreference -> ResponseEntity
                            .status(HttpStatus.CREATED)
                            .body((DataResponse) entityResponse(paymentPreference)))
                    .getOrElseGet(error -> Match(error).of(
                            Case($(MEMBERSHIP_ACQUISITION_NOT_FOUND), notFound),
                            Case($(MEMBERSHIP_ACQUISITION_FORBIDDEN), forbidden),
                            Case($(MEMBERSHIP_ACQUISITION_IS_NOT_PENDING), isNotPending)
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(invalid("INVALID_MEMBERSHIP_ACQUISITION_ID", "The id provided is invalid"));
        }
    }

    @PostMapping("/api/membership_acquisitions/{membershipAcquisitionId}/mp_notifications/")
    public ResponseEntity<?> mpNotification(
            @PathVariable String membershipAcquisitionId,
            @RequestBody MercadoPagoPaymentNotification notification
    ) {
        LoggerFactory.getLogger(getClass()).info(format("MEMBERSHIP_ACQUISITION_ID: %s", membershipAcquisitionId));
        LoggerFactory.getLogger(getClass()).info(notification.toString());
        if (notification.getAction().equals("payment.created") || notification.getAction().equals("payment.updated"))
            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition(membershipAcquisitionId,
                    notification);
        return ResponseEntity.ok().build();
    }
}
