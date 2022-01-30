package backoffice.application.dto.equipment;

import lombok.Value;

@Value(staticConstructor = "of")
public class EquipmentResponse {
    String id;
    String name;
    String category;
}
