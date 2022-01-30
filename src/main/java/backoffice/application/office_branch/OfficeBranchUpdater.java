package backoffice.application.office_branch;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import io.vavr.control.Either;
import shared.domain.EventBus;

import org.springframework.stereotype.Service;

@Service
public class OfficeBranchUpdater {

    private final EventBus                  eventBus;
    private final OfficeBranchRepository    officeBranchRepo;
    private final OfficeBranchAuthValidator officeBranchAuthValidator;

    public OfficeBranchUpdater(
            EventBus                  eventBus,
            OfficeBranchRepository    officeBranchRepo,
            OfficeBranchAuthValidator officeBranchAuthValidator
    ) {
        this.eventBus                  = eventBus;
        this.officeBranchRepo          = officeBranchRepo;
        this.officeBranchAuthValidator = officeBranchAuthValidator;
    }

    public Either<OfficeBranchError, Void> update(OfficeBranchId id, OfficeBranchUpdateInformation info) {
        return officeBranchRepo
                .findById(id)
                .toEither(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST)
                .filterOrElse(
                        officeBranchAuthValidator::authUserIsOwner,
                        ob -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .map(officeBranch -> officeBranch.update(info))
                .flatMap(officeBranchUpdated -> officeBranchRepo
                        .update(officeBranchUpdated)
                        .onSuccess(v -> eventBus.publish(officeBranchUpdated.officeBranchUpdatedEvent()))
                        .toEither(OfficeBranchError.DB_ERROR)
                );

    }
}
