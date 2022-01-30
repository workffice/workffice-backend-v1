package booking.domain.booking;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.persistence.Embeddable;

@Embeddable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@NoArgsConstructor
public class ScheduleTime {
    private LocalDate scheduleDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ZoneId zoneId;

    public static ScheduleTime create(LocalDateTime startTime, LocalDateTime endTime, ZoneId zoneId) {
        return new ScheduleTime(
                startTime.toLocalDate(),
                startTime.withSecond(0).withNano(0),
                endTime.withSecond(0).withNano(0),
                zoneId
        );
    }

    public boolean hasConflicts(
            LocalDateTime proposedStartTime,
            LocalDateTime proposedEndTime
    ) {
        var startHourHasConflicts =
                (proposedStartTime.isAfter(startTime) && proposedStartTime.isBefore(endTime));
        var endHourHasConflicts =
                (proposedEndTime.isAfter(startTime) && proposedEndTime.isBefore(endTime));
        var proposedTimeContainsScheduleTime =
                (proposedStartTime.equals(startTime) || proposedStartTime.isBefore(startTime)) &&
                        (proposedEndTime.equals(endTime) || proposedEndTime.isAfter(endTime));
        return startHourHasConflicts || endHourHasConflicts || proposedTimeContainsScheduleTime;
    }

    public LocalDateTime startTimeUTC() { return startTime; }

    public LocalDateTime endTimeUTC() { return endTime; }

    public ZonedDateTime startTime() {
        var utcTime = ZonedDateTime.of(startTime, ZoneId.of("UTC"));
        return utcTime.withZoneSameInstant(zoneId);
    }

    public ZonedDateTime endTime() {
        var utcTime = ZonedDateTime.of(endTime, ZoneId.of("UTC"));
        return utcTime.withZoneSameInstant(zoneId);
    }
}
