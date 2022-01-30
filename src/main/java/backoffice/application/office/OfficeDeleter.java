package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.domain.EventBus;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class OfficeDeleter {
    private final OfficeRepository    officeRepo;
    private final PermissionValidator permissionValidator;
    private final EventBus            eventBus;

    public OfficeDeleter(
            OfficeRepository    officeRepo,
            PermissionValidator permissionValidator,
            EventBus            eventBus
    ) {
        this.officeRepo          = officeRepo;
        this.permissionValidator = permissionValidator;
        this.eventBus            = eventBus;
    }

    public Either<OfficeError, Void> delete(OfficeId id) {
        var today = LocalDate.now(Clock.systemUTC());
        return officeRepo
                .findById(id)
                .filter(office -> !office.isDeleted(today))
                .toEither(OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)),
                        o -> OfficeError.OFFICE_FORBIDDEN)
                .map(office -> {
                    office.delete(today.plusMonths(2));
                    return office;
                })
                .flatMap(office -> officeRepo
                        .update(office)
                        .onSuccess(v -> eventBus.publish(office.officeDeletedEvent()))
                        .toEither(OfficeError.DB_ERROR));
    }
}
