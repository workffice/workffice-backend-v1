package backoffice.application.equipment;

import backoffice.application.dto.equipment.EquipmentError;
import backoffice.application.dto.equipment.EquipmentInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.equipment.Equipment;
import backoffice.domain.equipment.EquipmentCategory;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class EquipmentCreator {

    private final EquipmentRepository equipmentRepo;
    private final OfficeBranchFinder officeBranchFinder;


    public EquipmentCreator(
            EquipmentRepository equipmentRepo,
            OfficeBranchFinder officeBranchFinder
    ) {
        this.equipmentRepo = equipmentRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    private Equipment createEquipment(
            EquipmentId id,
            EquipmentInformation info,
            OfficeBranch officeBranch
    ) {
        return Equipment.create(
                id,
                info.getName(),
                EquipmentCategory.valueOf(info.getCategory()),
                officeBranch);
    }

    public Either<UseCaseError, Void> createEquipment(
            EquipmentId id,
            EquipmentInformation info,
            OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.EQUIPMENT))
                .map(OfficeBranch::fromDTO)
                .map(officeBranch -> createEquipment(id, info, officeBranch))
                .flatMap(equipment -> equipmentRepo.store(equipment).toEither(EquipmentError.DB_ERROR));
    }
}
