package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import org.springframework.stereotype.Service;

@Service
public class OfficeUpdater {
    private final EventBus            eventBus;
    private final OfficeRepository    officeRepo;
    private final PermissionValidator permissionValidator;

    public OfficeUpdater(
            EventBus            eventBus,
            OfficeRepository    officeRepo,
            PermissionValidator permissionValidator
    ) {
        this.eventBus            = eventBus;
        this.officeRepo          = officeRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<UseCaseError, Void> update(OfficeId id, OfficeUpdateInformation info) {
        return officeRepo
                .findById(id)
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)),
                        o -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .flatMap(office -> office.update(info).toEither(OfficeError.SHARED_OFFICE_WITHOUT_TABLES))
                .flatMap(officeUpdated -> officeRepo
                        .update(officeUpdated)
                        .onSuccess(v -> eventBus.publish(officeUpdated.officeUpdatedEvent()))
                        .toEither(OfficeError.DB_ERROR));
    }
}
