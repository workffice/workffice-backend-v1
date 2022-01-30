package backoffice.application.dto.office_branch;

import shared.application.UseCaseError;

public enum OfficeBranchError implements UseCaseError {
    OFFICE_BRANCH_NOT_EXIST,
    OFFICE_BRANCH_FORBIDDEN,
    OFFICE_BRANCH_HAS_CREATED_OFFICES,
    DB_ERROR
}
