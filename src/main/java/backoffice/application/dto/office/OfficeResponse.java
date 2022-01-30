package backoffice.application.dto.office;

import backoffice.application.dto.equipment.EquipmentResponse;
import backoffice.application.dto.service.ServiceResponse;
import lombok.Value;

import java.time.LocalDate;
import java.util.Set;

@Value(staticConstructor = "of")
public class OfficeResponse {
    String                 id;
    String                 name;
    String                 description;
    Integer                capacity;
    Integer                price;
    String                 imageUrl;
    String                 privacy;
    LocalDate              deletedAt;
    TableResponse          table;
    Set<ServiceResponse>   services;
    Set<EquipmentResponse> equipments;

    @Value(staticConstructor = "of")
    public static class TableResponse {
        Integer quantity;
        Integer capacity;
    }
}
