package backoffice.application.dto.office_holder;

import shared.application.UseCaseError;

public enum OfficeHolderError implements UseCaseError {
    OFFICE_HOLDER_NOT_FOUND,
    OFFICE_HOLDER_FORBIDDEN
}
