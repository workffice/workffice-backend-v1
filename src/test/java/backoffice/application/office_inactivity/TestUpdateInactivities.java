package backoffice.application.office_inactivity;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_inactivity.InactivityError;
import backoffice.application.dto.office_inactivity.InactivityInformation;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_inactivity.InactivityCreatedEvent;
import backoffice.domain.office_inactivity.InactivityDeletedEvent;
import backoffice.domain.office_inactivity.InactivityRepository;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.InactivityBuilder;
import backoffice.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestUpdateInactivities {
    InactivityRepository inactivityRepo = mock(InactivityRepository.class);
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    EventBus eventBus = mock(EventBus.class);

    InactivitiesUpdater updater = new InactivitiesUpdater(inactivityRepo, officeRepo, permissionValidator, eventBus);

    @Test
    void itShouldReturnOfficeNotFoundWhenThereIsNoOfficeWithIdSpecified() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = updater.updateOfficeInactivities(officeId, ImmutableList.of());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeForbiddenWhenAuthUserDoesNotHaveAccessToIt() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(false);

        Either<UseCaseError, Void> response = updater.updateOfficeInactivities(office.id(), ImmutableList.of());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_FORBIDDEN);
    }

    @Test
    void itShouldReturnInactivityMismatchWithDateWhenThereIsAtLeastOneInactivityWithInvalidInfoFormat() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        var info = InactivityInformation.of(
                InactivityType.RECURRING_DAY.name(),
                null,
                LocalDate.now()
        );

        Either<UseCaseError, Void> response = updater.updateOfficeInactivities(
                office.id(),
                ImmutableList.of(info)
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE);
    }

    @Test
    void itShouldDeleteInactivitiesThatAreNotSpecifiedInTheNewInactivities() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        when(inactivityRepo.findAllByOffice(any())).thenReturn(ImmutableList.of(
                new InactivityBuilder().withDayOfWeek(DayOfWeek.THURSDAY).build(),
                new InactivityBuilder().withDayOfWeek(DayOfWeek.MONDAY).build(),
                new InactivityBuilder().withDayOfWeek(DayOfWeek.FRIDAY).build()
        ));
        when(inactivityRepo.bulkStore(any())).thenReturn(Try.success(null));
        when(inactivityRepo.delete(any())).thenReturn(Try.success(null));

        var info = InactivityInformation.of(
                InactivityType.RECURRING_DAY.name(),
                DayOfWeek.MONDAY,
                null
        );

        Either<UseCaseError, Void> response = updater.updateOfficeInactivities(
                office.id(),
                ImmutableList.of(info)
        );

        assertThat(response.isRight()).isTrue();
        verify(inactivityRepo, times(1)).delete(ImmutableList.of(
                new InactivityBuilder().withDayOfWeek(DayOfWeek.THURSDAY).build(),
                new InactivityBuilder().withDayOfWeek(DayOfWeek.FRIDAY).build()
        ));
        verify(eventBus, times(2)).publish(any(InactivityDeletedEvent.class));
    }

    @Test
    void itShouldStoreInactivitiesThatDoNotExistPreviously() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        when(inactivityRepo.findAllByOffice(any())).thenReturn(ImmutableList.of(
                new InactivityBuilder().withDayOfWeek(DayOfWeek.THURSDAY).build(),
                new InactivityBuilder().withDayOfWeek(DayOfWeek.MONDAY).build(),
                new InactivityBuilder().withDayOfWeek(DayOfWeek.FRIDAY).build()
        ));
        when(inactivityRepo.bulkStore(any())).thenReturn(Try.success(null));
        when(inactivityRepo.delete(any())).thenReturn(Try.success(null));

        var info = InactivityInformation.of(
                InactivityType.RECURRING_DAY.name(),
                DayOfWeek.MONDAY,
                null
        );
        var info2 = InactivityInformation.of(
                InactivityType.RECURRING_DAY.name(),
                DayOfWeek.TUESDAY,
                null
        );

        Either<UseCaseError, Void> response = updater.updateOfficeInactivities(
                office.id(),
                ImmutableList.of(info, info2)
        );

        assertThat(response.isRight()).isTrue();
        verify(inactivityRepo, times(1)).bulkStore(ImmutableList.of(
                new InactivityBuilder().withDayOfWeek(DayOfWeek.TUESDAY).build()
        ));
        verify(eventBus, times(1)).publish(any(InactivityCreatedEvent.class));
    }
}
