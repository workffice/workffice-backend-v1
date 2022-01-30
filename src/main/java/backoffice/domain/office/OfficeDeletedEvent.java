package backoffice.domain.office;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = false)
@Getter
public class OfficeDeletedEvent extends DomainEvent {
    String officeBranchId;
    String officeId;

    @Override
    public String getEventName() {
        return "OFFICE_DELETED";
    }
}
