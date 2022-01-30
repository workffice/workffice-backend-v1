package review.application.dto;

import shared.application.UseCaseError;

public enum ReviewError implements UseCaseError {
    REVIEW_FORBIDDEN,
    NO_BOOKING,
    REVIEW_ALREADY_CREATED,
    INVALID_REVIEW_INFO,
    DB_ERROR
}
