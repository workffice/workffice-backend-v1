package backoffice.application.equipment;

import backoffice.application.dto.equipment.EquipmentResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.equipment.Equipment;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EquipmentFinder {

    private final EquipmentRepository equipmentRepository;
    private final OfficeBranchFinder officeBranchFinder;

    public EquipmentFinder(EquipmentRepository equipmentRepository, OfficeBranchFinder officeBranchFinder) {
        this.equipmentRepository = equipmentRepository;
        this.officeBranchFinder = officeBranchFinder;
    }

    private List<EquipmentResponse> toEquipmentResponses(List<Equipment> equipments) {
        return equipments.stream()
                .map(Equipment::toResponse)
                .collect(Collectors.toList());
    }

    public Either<UseCaseError, List<EquipmentResponse>> find(OfficeBranchId officeBranchId) {
        return officeBranchFinder
                .find(officeBranchId)
                .toEither((UseCaseError) OfficeBranchError.OFFICE_BRANCH_NOT_EXIST)
                .map(OfficeBranch::fromDTO)
                .map(equipmentRepository::findByOfficeBranch)
                .map(this::toEquipmentResponses);
    }
}
