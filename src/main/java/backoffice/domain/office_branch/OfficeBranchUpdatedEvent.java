package backoffice.domain.office_branch;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

import java.util.List;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = false)
@Getter
public class OfficeBranchUpdatedEvent extends DomainEvent {
    private final String id;
    private final String name;
    private final String province;
    private final String city;
    private final String street;
    private final String phone;
    private final List<String> imageUrls;

    @Override
    public String getEventName() {
        return "OFFICE_BRANCH_UPDATED";
    }
}
