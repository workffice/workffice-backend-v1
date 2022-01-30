package booking.domain.office.privacy;

import booking.domain.booking.Booking;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "PRIVATE")
@NoArgsConstructor
public class PrivateOffice extends Privacy {
    @Column
    private Integer capacity;

    public PrivateOffice(Integer capacity) {
        this.capacity = capacity;
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
        return existentBookings.stream().noneMatch(booking -> booking.hasConflictsWithProposedTime(
                proposedStartTime,
                proposedEndTime
        ));
    }
}
