package booking.domain.inactivity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "RECURRING_DAY")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RecurringDay extends Inactivity {
    @Column
    private DayOfWeek dayOfWeekUnavailable;

    public RecurringDay(InactivityId id, DayOfWeek dayOfWeekUnavailable) {
        this.id = id;
        this.dayOfWeekUnavailable = dayOfWeekUnavailable;
    }

    @Override
    public boolean isUnavailableAt(LocalDate date) {
        return date.getDayOfWeek().equals(dayOfWeekUnavailable);
    }
}
