package backoffice.domain.equipment;

import shared.domain.DomainId;

import java.util.UUID;

public class EquipmentId extends DomainId {

    public EquipmentId() {
        super();
    }

    public EquipmentId(UUID id) {
        super(id);
    }

    public static EquipmentId fromString(String id) {
        return new EquipmentId(UUID.fromString(id));
    }
}
