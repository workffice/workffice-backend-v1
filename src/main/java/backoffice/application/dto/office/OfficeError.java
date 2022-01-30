package backoffice.application.dto.office;

import shared.application.UseCaseError;

public enum OfficeError implements UseCaseError {
    DB_ERROR,
    SHARED_OFFICE_WITHOUT_TABLES,
    OFFICE_NOT_FOUND,
    OFFICE_FORBIDDEN,
}
