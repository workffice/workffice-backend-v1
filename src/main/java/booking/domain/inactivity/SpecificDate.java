package booking.domain.inactivity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "SPECIFIC_DATE")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SpecificDate extends Inactivity {

    @Column
    private LocalDate unavailableDate;

    public SpecificDate(InactivityId id, LocalDate unavailableDate) {
        this.id = id;
        this.unavailableDate = unavailableDate;
    }

    @Override
    public boolean isUnavailableAt(LocalDate date) {
        return unavailableDate.equals(date);
    }
}
