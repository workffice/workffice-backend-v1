package booking.application.booking.creation;

import booking.application.dto.booking.BookingInformation;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SingleBookingCreator implements BookingCreationStrategy {
    private static final ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private final BookingRepository bookingRepo;

    public SingleBookingCreator(BookingRepository bookingRepo) {
        this.bookingRepo = bookingRepo;
    }

    private List<Booking> findExistentBookings(Office office, LocalDate scheduleDate) {
        return bookingRepo.find(office, scheduleDate)
                .stream().filter(Booking::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public Either<UseCaseError, Booking> book(Office office, String renterEmail, BookingInformation info) {
        var argStartTime = ZonedDateTime.of(info.getStartTime(), timezoneARG);
        var existentBookings = findExistentBookings(
                office,
                argStartTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDate()
        );
        return office.book(
                renterEmail,
                info.getAttendeesQuantity(),
                argStartTime,
                ZonedDateTime.of(info.getEndTime(), timezoneARG),
                existentBookings
        );
    }
}
