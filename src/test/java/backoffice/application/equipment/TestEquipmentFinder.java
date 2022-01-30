package backoffice.application.equipment;

import backoffice.application.dto.equipment.EquipmentResponse;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.factories.EquipmentBuilder;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestEquipmentFinder {

    EquipmentRepository equipmentRepo = mock(EquipmentRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    EquipmentFinder equipmentFinder = new EquipmentFinder(
            equipmentRepo,
            officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.find(officeBranchId)).thenReturn(Option.none());

        Either<UseCaseError, List<EquipmentResponse>> response = equipmentFinder.find(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnAllEquipmentsRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var equipment1 = new EquipmentBuilder().build();
        var equipment2 = new EquipmentBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        when(equipmentRepo.findByOfficeBranch(any())).thenReturn(ImmutableList.of(equipment1, equipment2));

        Either<UseCaseError, List<EquipmentResponse>> response = equipmentFinder.find(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(equipment1.toResponse(), equipment2.toResponse());
    }

}
