package backoffice.application.dto.role;

import shared.application.UseCaseError;

public enum RoleError implements UseCaseError {
    DB_ERROR,
    ROLE_NOT_FOUND,
    ROLE_FORBIDDEN
}
