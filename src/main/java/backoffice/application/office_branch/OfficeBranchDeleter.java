package backoffice.application.office_branch;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office.OfficesFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import io.vavr.control.Either;
import shared.domain.EventBus;

import org.springframework.stereotype.Service;

@Service
public class OfficeBranchDeleter {
    private final EventBus                  eventBus;
    private final OfficeBranchRepository    officeBranchRepo;
    private final OfficeBranchAuthValidator officeBranchAuthValidator;
    private final OfficesFinder             officesFinder;

    public OfficeBranchDeleter(
            EventBus                  eventBus,
            OfficeBranchRepository    officeBranchRepo,
            OfficeBranchAuthValidator officeBranchAuthValidator,
            OfficesFinder             officesFinder
    ) {
        this.eventBus                  = eventBus;
        this.officeBranchRepo          = officeBranchRepo;
        this.officeBranchAuthValidator = officeBranchAuthValidator;
        this.officesFinder             = officesFinder;
    }

    public Either<OfficeBranchError, Void> delete(OfficeBranchId id) {
        return officeBranchRepo
                .findById(id)
                .toEither(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST)
                .filterOrElse(
                        officeBranchAuthValidator::authUserIsOwner,
                        ob -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .filterOrElse(
                        officeBranch -> officesFinder.find(officeBranch.id()).get().isEmpty(),
                        officeBranch -> OfficeBranchError.OFFICE_BRANCH_HAS_CREATED_OFFICES)
                .map(OfficeBranch::delete)
                .flatMap(officeBranch -> officeBranchRepo
                        .update(officeBranch)
                        .onSuccess(v -> eventBus.publish(officeBranch.officeBranchDeletedEvent()))
                        .toEither(OfficeBranchError.DB_ERROR)
                );
    }
}
