package backoffice.application.dto.office;

import backoffice.domain.office.Privacy;
import controller.validators.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class OfficeInformation {
    @NotBlank(message = "Name is required")
    private String name;
    @NotNull(message = "Description must not be null")
    private String description;
    @NotNull(message = "Capacity is required")
    private Integer capacity;
    @NotNull(message = "Price is required")
    private Integer price;
    @NotBlank(message = "Privacy is required")
    @ValueOfEnum(enumClass = Privacy.class, message = "Privacy must be PRIVATE or SHARED")
    private String privacy;
    private String imageUrl;
    private Integer tablesQuantity;
    private Integer capacityPerTable;
}
