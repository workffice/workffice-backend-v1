package booking.domain.inactivity;

import java.time.LocalDate;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Table(name = "inactivities")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "inactivityType", discriminatorType = DiscriminatorType.STRING)
public abstract class Inactivity {
    @EmbeddedId
    protected InactivityId id;

    public InactivityId id() { return id; }

    public abstract boolean isUnavailableAt(LocalDate date);
}
