package booking.application.dto;

import shared.application.UseCaseError;

public enum OfficeError implements UseCaseError {
    OFFICE_NOT_FOUND,
    OFFICE_IS_DELETED,
}
