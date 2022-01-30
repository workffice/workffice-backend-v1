package authentication.application.dto.token;

import shared.application.UseCaseError;

public enum TokenError implements UseCaseError {
    INVALID_TOKEN,
    TOKEN_ALREADY_USED
}
