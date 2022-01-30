package backoffice.application.dto.office;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class OfficeUpdateInformation {
    private String name;
    private String description;
    private Integer capacity;
    private Integer price;
    private String privacy;
    private String imageUrl;
    private Integer tablesQuantity;
    private Integer capacityPerTable;

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<Integer> capacity() {
        return Optional.ofNullable(capacity);
    }

    public Optional<Integer> price() {
        return Optional.ofNullable(price);
    }

    public Optional<String> privacy() {
        return Optional.ofNullable(privacy);
    }

    public Optional<String> imageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    public Optional<Integer> tablesQuantity() {
        return Optional.ofNullable(tablesQuantity);
    }

    public Optional<Integer> capacityPerTable() {
        return Optional.ofNullable(capacityPerTable);
    }
}
