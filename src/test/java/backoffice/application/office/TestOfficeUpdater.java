package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office.OfficeUpdatedEvent;
import backoffice.domain.office.Privacy;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeUpdater {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    EventBus eventBus = mock(EventBus.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeUpdater updater = new OfficeUpdater(eventBus, officeRepo, permissionValidator);
    OfficeUpdateInformation info = OfficeUpdateInformation.of(
            "Updated name",
            "Second desc",
            100,
            1000,
            Privacy.PRIVATE.name(),
            "newimage.url",
            2,
            5
    );

    @Test
    void itShouldReturnOfficeNotFoundWhenThereIsNoOfficeWithIdSpecified() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = updater.update(officeId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserIsNoOwnerOrCollaborator() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(false);

        Either<UseCaseError, Void> response = updater.update(office.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnSharedOfficeWithoutTableErrorWhenTryingToUpdateToSharedOfficeWithoutTables() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        OfficeUpdateInformation info = OfficeUpdateInformation.of(
                "Updated name",
                "Second desc",
                100,
                1000,
                Privacy.SHARED.name(),
                "newimage.url",
                null,
                null
        );

        Either<UseCaseError, Void> response = updater.update(office.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.SHARED_OFFICE_WITHOUT_TABLES);
    }

    @Test
    void itShouldUpdateOfficeWithInformationSpecified() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        when(officeRepo.update(any(Office.class))).thenReturn(Try.success(null));

        Either<UseCaseError, Void> response = updater.update(office.id(), info);

        assertThat(response.isRight()).isTrue();
        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeUpdated = officeArgumentCaptor.getValue();
        var officeResponse = officeUpdated.toResponse();
        assertThat(officeResponse.getName()).isEqualTo("Updated name");
        assertThat(officeResponse.getDescription()).isEqualTo("Second desc");
        assertThat(officeResponse.getCapacity()).isEqualTo(100);
        assertThat(officeResponse.getPrice()).isEqualTo(1000);
        assertThat(officeResponse.getImageUrl()).isEqualTo("newimage.url");
        assertThat(officeResponse.getPrivacy()).isEqualTo(Privacy.PRIVATE.name());
        assertThat(officeResponse.getTable()).isEqualTo(OfficeResponse.TableResponse.of(2, 5));
    }

    @Test
    void itShouldDispatchOfficeUpdatedEventWhenOfficeISUpdatedSuccessfully() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        when(officeRepo.update(any(Office.class))).thenReturn(Try.success(null));

        Either<UseCaseError, Void> response = updater.update(office.id(), info);

        assertThat(response.isRight()).isTrue();
        verify(eventBus, times(1)).publish(
                OfficeUpdatedEvent.of(
                        office.id().toString(),
                        office.officeBranch().id().toString(),
                        "Updated name",
                        "PRIVATE",
                        1000,
                        100,
                        2,
                        5
                )
        );
    }
}
