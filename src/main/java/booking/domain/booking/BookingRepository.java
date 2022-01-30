package booking.domain.booking;

import booking.domain.office.Office;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository {

    Try<Void> store(Booking booking);

    Try<Void> update(Booking booking);

    Option<Booking> findById(BookingId id);

    List<Booking> find(Office office, LocalDate proposedScheduleDate);

    List<Booking> find(
            String renterEmail,
            boolean fetchCurrentBookings,
            LocalDate currentDate,
            Integer offset,
            Integer limit
    );

    Long count(String renterEmail, boolean fetchCurrentBookings, LocalDate currentDate);

    boolean exists(String renterEmail, Office officeId);
}
