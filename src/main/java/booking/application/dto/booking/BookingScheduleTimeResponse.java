package booking.application.dto.booking;

import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value(staticConstructor = "of")
public class BookingScheduleTimeResponse {
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
}
