package backoffice.application.dto.office_inactivity;

import shared.application.UseCaseError;

public enum InactivityError implements UseCaseError {
    DB_ERROR,
    INACTIVITY_TYPE_MISMATCH_WITH_DATE
}
