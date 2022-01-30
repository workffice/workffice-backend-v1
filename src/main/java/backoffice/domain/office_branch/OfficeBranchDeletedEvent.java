package backoffice.domain.office_branch;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = false)
@Getter
public class OfficeBranchDeletedEvent extends DomainEvent {
    private final String officeBranchId;

    @Override
    public String getEventName() {
        return "OFFICE_BRANCH_DELETED";
    }
}
