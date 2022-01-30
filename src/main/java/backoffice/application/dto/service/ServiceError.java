package backoffice.application.dto.service;

import shared.application.UseCaseError;

public enum ServiceError implements UseCaseError {
    SERVICE_ALREADY_EXISTS,
    DB_ERROR
}
