package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OfficeEquipmentUpdater {
    private final OfficeRepository    officeRepo;
    private final EquipmentRepository equipmentRepo;
    private final PermissionValidator permissionValidator;

    public OfficeEquipmentUpdater(
            OfficeRepository    officeRepo,
            EquipmentRepository equipmentRepo,
            PermissionValidator permissionValidator
    ) {
        this.officeRepo          = officeRepo;
        this.equipmentRepo       = equipmentRepo;
        this.permissionValidator = permissionValidator;
    }

    public Either<OfficeError, Void> update(OfficeId id, Set<EquipmentId> equipmentIds) {
        return officeRepo
                .findById(id)
                .toEither(OfficeError.OFFICE_NOT_FOUND)
                .filterOrElse(
                        office -> permissionValidator.userHasPerms(
                                office.officeBranch(),
                                Permission.create(Access.WRITE, Resource.OFFICE)),
                        o -> OfficeError.OFFICE_FORBIDDEN)
                .map(office -> {
                    var equipments = equipmentRepo.findByOfficeBranch(office.officeBranch());
                    var equipmentsToAssociate = equipments
                            .stream()
                            .filter(e -> equipmentIds.contains(e.id()))
                            .collect(Collectors.toSet());
                    office.setEquipments(equipmentsToAssociate);
                    return office;
                })
                .flatMap(office -> officeRepo.update(office).toEither(OfficeError.DB_ERROR));
    }
}
