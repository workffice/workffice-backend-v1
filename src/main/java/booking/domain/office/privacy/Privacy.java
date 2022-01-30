package booking.domain.office.privacy;

import booking.domain.booking.Booking;
import io.vavr.control.Try;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Table(name = "privacies")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "privacyType", discriminatorType = DiscriminatorType.STRING)
public abstract class Privacy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static Try<Privacy> createPrivacy(String privacy, Integer capacity, Integer tablesQuantity,
                                       Integer capacityPerTable) {
        if (privacy.equals("PRIVATE"))
            return Try.success(new PrivateOffice(capacity));
        else if (privacy.equals("SHARED"))
            return Try.success(new SharedOffice(tablesQuantity, capacityPerTable));
        else
            return Try.failure(new IllegalArgumentException("Privacy type is invalid"));
    }

    /**
     * @param proposedStartTime Start time in UTC
     * @param proposedEndTime End time in UTC
     */
    public abstract boolean canBeBooked(
            LocalDateTime proposedStartTime,
            LocalDateTime proposedEndTime,
            List<Booking> existentBookings
    );
}
