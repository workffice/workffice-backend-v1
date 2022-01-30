package backoffice.domain.office_inactivity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode(callSuper = false)
public class InactivityDeletedEvent extends DomainEvent {
    private final String inactivityId;

    @Override
    public String getEventName() {
        return "INACTIVITY_DELETED_EVENT";
    }
}
