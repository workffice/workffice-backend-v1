package booking.application.booking;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.UserError;
import booking.application.booking.creation.BookingCreationStrategy;
import booking.application.dto.OfficeError;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingInformation;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import io.vavr.Function3;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class BookingCreator {
    private final AuthUserFinder          authUserFinder;
    private final OfficeRepository        officeRepo;
    private final BookingRepository       bookingRepo;
    private final BookingCreationStrategy bookingCreationStrategy;

    public BookingCreator(
            AuthUserFinder          authUserFinder,
            OfficeRepository        officeRepo,
            BookingRepository       bookingRepo,
            BookingCreationStrategy bookingCreationStrategy
    ) {
        this.authUserFinder          = authUserFinder;
        this.officeRepo              = officeRepo;
        this.bookingRepo             = bookingRepo;
        this.bookingCreationStrategy = bookingCreationStrategy;
    }

    private Function3<Office, String, BookingInformation, Either<UseCaseError, Booking>> createBooking() {
        return bookingCreationStrategy::book;
    }

    public Either<UseCaseError, BookingResponse> create(OfficeId officeId, BookingInformation info) {
        return officeRepo.findById(officeId)
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> !office.isDeleted(),
                        office -> OfficeError.OFFICE_IS_DELETED
                )
                .map(office -> createBooking().curried().apply(office))
                .flatMap(createBooking -> authUserFinder
                        .findAuthenticatedUser()
                        .toEither((UseCaseError) UserError.USER_NOT_FOUND)
                        .map(user -> createBooking.apply(user.getEmail())))
                .flatMap(createBooking -> createBooking.apply(info))
                .flatMap(booking -> bookingRepo.store(booking)
                        .toEither((UseCaseError) BookingError.DB_ERROR)
                        .map(v -> booking))
                .map(Booking::toResponse);
    }
}
