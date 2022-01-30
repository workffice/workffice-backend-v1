package backoffice.domain.office;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(of = {"quantity", "capacityPerTable"})
@NoArgsConstructor
public class Tables {
    private Integer quantity;
    private Integer capacityPerTable;
    
    private Tables(Integer quantity, Integer capacityPerTable) {
        this.quantity = quantity;
        this.capacityPerTable = capacityPerTable;
    }
    
    public static Tables create(Integer quantity, Integer capacityPerTable) {
        return new Tables(quantity, capacityPerTable);
    }
    
    public Integer quantity() { return quantity; }
    
    public Integer capacityPerTable() { return capacityPerTable; }
    
    public boolean hasAllInformation() {
        return quantity != null && capacityPerTable != null;
    }
    
    public boolean hasEmptyInformation() {
        return quantity == null && capacityPerTable == null;
    }
}
