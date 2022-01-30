package search.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import search.application.dto.OfficeBranchResponse;

@AllArgsConstructor(staticName = "create")
@EqualsAndHashCode
public class Office {
    private final String        id;
    private       String        name;
    private       Integer       price;
    private       Integer       capacity;
    private       Integer       tablesQuantity;
    private       Integer       capacityPerTable;
    private       OfficePrivacy privacy;

    public String id() { return  id; }

    public OfficeBranchResponse.OfficeResponse toResponse() {
        return OfficeBranchResponse.OfficeResponse.of(
                id,
                name,
                price,
                capacity,
                tablesQuantity,
                capacityPerTable,
                privacy.name()
        );
    }

    public void update(
            String name,
            Integer price,
            Integer capacity,
            Integer tablesQuantity,
            Integer capacityPerTable,
            OfficePrivacy privacy
    ) {
        this.name = name;
        this.price = price;
        this.capacity = capacity;
        this.tablesQuantity = tablesQuantity;
        this.capacityPerTable = capacityPerTable;
        this.privacy = privacy;
    }
}
