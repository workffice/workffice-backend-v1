package backoffice.domain.office_inactivity;

import backoffice.domain.office.Office;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static backoffice.domain.office_inactivity.InactivityType.RECURRING_DAY;
import static backoffice.domain.office_inactivity.InactivityType.SPECIFIC_DATE;

@Entity
@Table(name = "inactivities")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"type", "dayOfWeek", "specificInactivityDay"})
public class Inactivity {
    @EmbeddedId
    private InactivityId id;
    @Enumerated(EnumType.STRING)
    private InactivityType type;
    @Column(nullable = true)
    private DayOfWeek dayOfWeek;
    @Column(nullable = true)
    private LocalDate specificInactivityDay;
    @ManyToOne(fetch = FetchType.LAZY)
    private Office office;

    public static Try<Inactivity> create(
            InactivityId id,
            InactivityType type,
            Option<DayOfWeek> dayOfWeek,
            Option<LocalDate> specificInactivityDay,
            Office office
    ) {
        if (type.equals(RECURRING_DAY) && dayOfWeek.isEmpty())
            return Try.failure(new IllegalArgumentException());
        if (type.equals(SPECIFIC_DATE) && specificInactivityDay.isEmpty())
            return Try.failure(new IllegalArgumentException());
        return Try.success(new Inactivity(
                id,
                type,
                dayOfWeek.getOrNull(),
                specificInactivityDay.getOrNull(),
                office
        ));
    }

    public InactivityCreatedEvent inactivityCreatedEvent() {
        return InactivityCreatedEvent.of(
                id.toString(),
                office.id().toString(),
                type.name(),
                dayOfWeek(),
                specificInactivityDay()
        );
    }

    public InactivityDeletedEvent inactivityDeletedEvent() {
        return InactivityDeletedEvent.of(id.toString());
    }

    public InactivityId id() { return id; }

    public Option<DayOfWeek> dayOfWeek() { return Option.of(dayOfWeek); }

    public Option<LocalDate> specificInactivityDay() { return Option.of(specificInactivityDay); }

    public InactivityType type() { return type; }
}
