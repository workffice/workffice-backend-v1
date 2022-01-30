package booking.application.dto.booking;

import shared.application.UseCaseError;

public enum BookingError implements UseCaseError {
    INVALID_SCHEDULE_TIME,
    OFFICE_IS_NOT_AVAILABLE,
    DB_ERROR,
    BOOKING_NOT_FOUND,
    BOOKING_IS_NOT_PENDING,
    BOOKING_FORBIDDEN,
    MERCADO_PAGO_ERROR
}
