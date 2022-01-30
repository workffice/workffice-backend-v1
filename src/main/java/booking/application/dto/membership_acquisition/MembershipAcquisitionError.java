package booking.application.dto.membership_acquisition;

import shared.application.UseCaseError;

public enum MembershipAcquisitionError implements UseCaseError {
    NO_AUTHENTICATED_USER,
    MEMBERSHIP_NOT_FOUND,
    MEMBERSHIP_ACQUISITION_NOT_FOUND,
    MEMBERSHIP_ACQUISITION_FORBIDDEN,
    MEMBERSHIP_ACQUISITION_IS_NOT_PENDING,
    MEMBERSHIP_ACQUISITION_IS_NOT_ACTIVE,
    DB_ERROR,
    MERCADO_PAGO_ERROR
}
