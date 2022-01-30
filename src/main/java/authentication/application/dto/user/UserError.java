package authentication.application.dto.user;

import shared.application.UseCaseError;

public enum UserError implements UseCaseError {
    USER_NOT_FOUND,
    DB_ERROR,
    NON_ACTIVE_USER,
    INVALID_PASSWORD,
    USER_EMAIL_ALREADY_EXISTS,
    FORBIDDEN,
}
