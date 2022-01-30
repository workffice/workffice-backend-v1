package backoffice.application.dto.equipment;

import shared.application.UseCaseError;

public enum EquipmentError implements  UseCaseError {
    EQUIPMENT_ALREADY_EXISTS,
    DB_ERROR
}
