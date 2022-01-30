package backoffice.application.office_inactivity;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_inactivity.InactivityError;
import backoffice.application.dto.office_inactivity.InactivityInformation;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityRepository;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import org.springframework.stereotype.Service;

@Service
public class InactivityCreator {
    
    private final OfficeRepository     officeRepo;
    private final InactivityRepository inactivityRepo;
    private final PermissionValidator  permissionValidator;
    private final EventBus             eventBus;

    public InactivityCreator(
            OfficeRepository     officeRepo,
            InactivityRepository inactivityRepo,
            PermissionValidator  permissionValidator,
            EventBus             eventBus
    ) {
        this.officeRepo          = officeRepo;
        this.inactivityRepo      = inactivityRepo;
        this.permissionValidator = permissionValidator;
        this.eventBus            = eventBus;
    }
    
    private Either<UseCaseError, Inactivity> createInactivity(
            InactivityId id,
            InactivityInformation info,
            Office office
    ) {
        Try<Inactivity> inactivity = Inactivity.create(
                id,
                InactivityType.valueOf(info.getType()),
                Option.of(info.getDayOfWeek()),
                Option.of(info.getSpecificInactivityDay()),
                office
        );
        return inactivity
                .toEither(InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE);
    }
    
    public Either<UseCaseError, Void> create(OfficeId officeId, InactivityId inactivityId, InactivityInformation info) {
        return officeRepo.findById(officeId)
                .toEither((UseCaseError) OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)
                        ), office -> OfficeBranchError.OFFICE_BRANCH_FORBIDDEN)
                .flatMap(office -> createInactivity(inactivityId, info, office))
                .flatMap(inactivity -> inactivityRepo
                        .store(inactivity)
                        .onSuccess(v -> eventBus.publish(inactivity.inactivityCreatedEvent()))
                        .toEither(InactivityError.DB_ERROR));
    }
}
