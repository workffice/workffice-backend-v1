package booking.application.booking.creation;

import booking.application.dto.booking.BookingInformation;
import booking.domain.booking.Booking;
import booking.domain.office.Office;
import io.vavr.control.Either;
import shared.application.UseCaseError;

public interface BookingCreationStrategy {

    Either<UseCaseError, Booking> book(Office office, String renterEmail, BookingInformation info);
}
