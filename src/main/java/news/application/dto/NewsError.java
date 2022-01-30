package news.application.dto;

import shared.application.UseCaseError;

public enum NewsError implements UseCaseError {
    DB_ERROR,
    NEWS_NOT_FOUND,
    NEWS_FORBIDDEN,
    NEWS_IS_NOT_DRAFT
}
