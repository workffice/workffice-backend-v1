package booking.domain.office.privacy;

import booking.domain.booking.Booking;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "SHARED")
@NoArgsConstructor
public class SharedOffice extends Privacy {
    @Column
    private Integer tablesQuantity;
    @Column
    private Integer capacityPerTable;

    public SharedOffice(Integer tablesQuantity, Integer capacityPerTable) {
        this.tablesQuantity   = tablesQuantity;
        this.capacityPerTable = capacityPerTable;
    }

    /**
     * @param proposedStartTime Start time in UTC
     * @param proposedEndTime End time in UTC
     */
    @Override
    public boolean canBeBooked(
            LocalDateTime proposedStartTime,
            LocalDateTime proposedEndTime,
            List<Booking> existentBookings
    ) {
        var bookingsWithConflict = existentBookings
                .stream()
                .filter(booking -> booking.hasConflictsWithProposedTime(proposedStartTime, proposedEndTime))
                .collect(Collectors.toList());
        if (bookingsWithConflict.size() < tablesQuantity)
            return true;

        Function<Booking, Integer> conflictsQuantityWithExistentBookings =
                bookingWithConflict -> (int) bookingsWithConflict
                        .stream()
                        .filter(booking -> booking.hasConflictsWithProposedTime(
                                bookingWithConflict.startScheduleTimeUTC(),
                                bookingWithConflict.endScheduleTimeUTC()
                        )).count();
        return bookingsWithConflict
                .stream()
                .map(conflictsQuantityWithExistentBookings)
                .noneMatch(conflictsQuantity -> conflictsQuantity >= tablesQuantity);
    }
}
