package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.service.ServiceId;
import backoffice.domain.service.ServiceRepository;
import io.vavr.control.Either;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OfficeServiceUpdater {
    private final OfficeRepository    officeRepo;
    private final ServiceRepository   serviceRepo;
    private final PermissionValidator permissionValidator;

    public OfficeServiceUpdater(
            OfficeRepository    officeRepo,
            ServiceRepository   serviceRepo,
            PermissionValidator permissionValidator
    ) {
        this.officeRepo          = officeRepo;
        this.serviceRepo         = serviceRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<OfficeError, Void> update(OfficeId id, Set<ServiceId> serviceIds) {
        return officeRepo
                .findById(id)
                .toEither(OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)),
                        o -> OfficeError.OFFICE_FORBIDDEN)
                .map(office -> {
                    var services = serviceRepo.findByOfficeBranch(office.officeBranch());
                    var servicesToAssociate = services
                            .stream()
                            .filter(s -> serviceIds.contains(s.id()))
                            .collect(Collectors.toSet());
                    office.setServices(servicesToAssociate);
                    return office;
                })
                .flatMap(office -> officeRepo.update(office).toEither(OfficeError.DB_ERROR));
    }
}
