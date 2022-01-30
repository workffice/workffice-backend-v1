package backoffice.application.dto.collaborator;

import shared.application.UseCaseError;

public enum CollaboratorError implements UseCaseError {
    DB_ERROR,
    COLLABORATOR_ALREADY_EXISTS,
    INVALID_TOKEN,
    COLLABORATOR_NOT_FOUND,
    FORBIDDEN,
}
