package backoffice.domain.office_inactivity;

import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode(callSuper = false)
public class InactivityCreatedEvent extends DomainEvent {
    private final String            inactivityId;
    private final String            officeId;
    private final String            inactivityType;
    private final Option<DayOfWeek> dayOfWeek;
    private final Option<LocalDate> specificInactivityDay;

    @Override
    public String getEventName() {
        return "INACTIVITY_CREATED_EVENT";
    }
}
