package backoffice.application.dto.equipment;

import backoffice.domain.equipment.EquipmentCategory;
import controller.validators.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class EquipmentInformation {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Category is required")
    @ValueOfEnum(enumClass = EquipmentCategory.class, message = "Category must be an option available")
    private String category;
}
