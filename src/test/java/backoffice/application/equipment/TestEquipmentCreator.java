package backoffice.application.equipment;

import backoffice.application.dto.equipment.EquipmentError;
import backoffice.application.dto.equipment.EquipmentInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.equipment.Equipment;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestEquipmentCreator {

    EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    ArgumentCaptor<Equipment> equipmentArgCaptor = ArgumentCaptor.forClass(Equipment.class);

    EquipmentCreator equipmentCreator = new EquipmentCreator(equipmentRepository, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        OfficeBranchId officeBranchId = new OfficeBranchId();
        EquipmentId equipmentId = new EquipmentId();
        EquipmentInformation equipmentInformation = EquipmentInformation.of(
                "Some name",
                "TECHNOLOGY"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.EQUIPMENT)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        var response = equipmentCreator.createEquipment(equipmentId, equipmentInformation, officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthenticatedUserHasNoAccessToOfficeBranch() {
        EquipmentId equipmentId = new EquipmentId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        EquipmentInformation equipmentInformation = EquipmentInformation.of(
                "Some name",
                "TECHNOLOGY"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.EQUIPMENT)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        var response = equipmentCreator.createEquipment(equipmentId, equipmentInformation, officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnDBErrorWhenStoreFails() {

        EquipmentId equipmentId = new EquipmentId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        EquipmentInformation equipmentInformation = EquipmentInformation.of(
                "Some name",
                "TECHNOLOGY"
        );

        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.EQUIPMENT)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(equipmentRepository.store(any())).thenReturn(Try.failure(new PersistenceException()));

        Either<UseCaseError, Void> response = equipmentCreator.createEquipment(
                equipmentId,
                equipmentInformation,
                officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(EquipmentError.DB_ERROR);
    }

    @Test
    void itShouldCallStoreEquipment() {
        EquipmentId equipmentId = new EquipmentId();
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        EquipmentInformation equipmentInformation = EquipmentInformation.of(
                "Some name",
                "TECHNOLOGY"
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.EQUIPMENT)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(equipmentRepository.store(any())).thenReturn(Try.success(null));

        equipmentCreator.createEquipment(equipmentId, equipmentInformation, officeBranch.id());

        verify(equipmentRepository, times(1)).store(equipmentArgCaptor.capture());
        Equipment equipmentStored = equipmentArgCaptor.getValue();
        var equipmentResponse = equipmentStored.toResponse();
        assertThat(equipmentResponse.getId()).isEqualTo(equipmentId.toString());
        assertThat(equipmentResponse.getName()).isEqualTo("Some name");
        assertThat(equipmentResponse.getCategory()).isEqualTo("TECHNOLOGY");
    }
}
