package backoffice.domain.office;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(callSuper = false)
@Getter
public class OfficeUpdatedEvent extends DomainEvent {
    private final String  id;
    private final String officeBranchId;
    private final String  name;
    private final String  privacy;
    private final Integer price;
    private final Integer capacity;
    private final Integer tablesQuantity;
    private final Integer capacityPerTable;

    @Override
    public String getEventName() {
        return "OFFICE_UPDATED";
    }
}
