package booking.application.booking;

import booking.application.dto.OfficeError;
import booking.application.dto.booking.BookingScheduleTimeResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import io.vavr.control.Either;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BookingScheduleTimeFinder {
    private final OfficeRepository officeRepo;
    private final BookingRepository bookingRepo;

    public BookingScheduleTimeFinder(OfficeRepository officeRepo, BookingRepository bookingRep) {
        this.bookingRepo = bookingRep;
        this.officeRepo = officeRepo;
    }

    public Either<OfficeError, List<BookingScheduleTimeResponse>> findBookingScheduledTimes(
            OfficeId officeId,
            LocalDate date
    ) {
        return officeRepo.findById(officeId)
                .toEither(OfficeError.OFFICE_NOT_FOUND)
                .map(office -> bookingRepo.find(office, date))
                .map(bookings -> bookings
                        .stream()
                        .filter(Booking::isActive)
                        .map(Booking::toScheduleTimeResponse)
                        .collect(Collectors.toList())
                );
    }
}
