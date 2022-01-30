package backoffice.application.dto.service;

import backoffice.domain.service.ServiceCategory;
import controller.validators.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ServiceInformation {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Category is required")
    @ValueOfEnum(enumClass = ServiceCategory.class, message = "Category must be an option available")
    private String category;
}
