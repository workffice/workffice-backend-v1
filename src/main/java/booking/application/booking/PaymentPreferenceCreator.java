package booking.application.booking;

import authentication.application.AuthUserValidator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingPreferenceInformation;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.domain.payment_preference.PaymentPreference;
import booking.domain.payment_preference.PreferenceCreator;
import io.vavr.control.Either;

import org.springframework.stereotype.Service;

@Service
public class PaymentPreferenceCreator {
    private final BookingRepository bookingRepo;
    private final AuthUserValidator authUserValidator;
    private final PreferenceCreator preferenceCreator;

    public PaymentPreferenceCreator(
            BookingRepository bookingRepo,
            AuthUserValidator authUserValidator,
            PreferenceCreator preferenceCreator
    ) {
        this.bookingRepo = bookingRepo;
        this.authUserValidator = authUserValidator;
        this.preferenceCreator = preferenceCreator;
    }

    public Either<BookingError, PaymentPreference> create(BookingId bookingId) {
        return bookingRepo.findById(bookingId)
                .toEither(BookingError.BOOKING_NOT_FOUND)
                .filterOrElse(
                        booking -> authUserValidator.isSameUserAsAuthenticated(booking.renterEmail()),
                        b -> BookingError.BOOKING_FORBIDDEN)
                .filterOrElse(
                        Booking::isPending,
                        b -> BookingError.BOOKING_IS_NOT_PENDING)
                .flatMap(booking -> preferenceCreator.createForBooking(
                        BookingPreferenceInformation.of(
                                booking.id().toString(),
                                booking.renterEmail(),
                                booking.office().name(),
                                (float) booking.totalAmount(),
                                booking.description()
                        )
                ));
    }
}
