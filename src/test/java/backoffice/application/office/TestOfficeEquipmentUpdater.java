package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.EquipmentBuilder;
import backoffice.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeEquipmentUpdater {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    EquipmentRepository equipmentRepo = mock(EquipmentRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeEquipmentUpdater updater = new OfficeEquipmentUpdater(officeRepo, equipmentRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<OfficeError, Void> response = updater.update(officeId, ImmutableSet.of(new EquipmentId()));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveWritePermissionToOffice() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(false);

        Either<OfficeError, Void> response = updater.update(office.id(), ImmutableSet.of(new EquipmentId()));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_FORBIDDEN);
    }

    @Test
    void itShouldAssociateServicesThatBelongToOfficeBranch() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        var equipment1 = new EquipmentBuilder().build();
        var equipment2 = new EquipmentBuilder().build();
        when(equipmentRepo.findByOfficeBranch(any())).thenReturn(ImmutableList.of(equipment1, equipment2));
        when(officeRepo.update(any())).thenReturn(Try.success(null));

        Either<OfficeError, Void> response = updater.update(office.id(), ImmutableSet.of(new EquipmentId(),
                equipment1.id(), equipment2.id()));

        assertThat(response.isRight()).isTrue();
        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeUpdated = officeArgumentCaptor.getValue();
        assertThat(officeUpdated.toResponse().getEquipments()).containsExactlyInAnyOrder(
                equipment1.toResponse(),
                equipment2.toResponse()
        );
    }
}
