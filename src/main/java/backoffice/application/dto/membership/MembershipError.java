package backoffice.application.dto.membership;

import shared.application.UseCaseError;

public enum MembershipError implements UseCaseError {
    DB_ERROR,
    MEMBERSHIP_NOT_FOUND,
    MEMBERSHIP_FORBIDDEN
}
