package backoffice.application.office;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeDeletedEvent;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.domain.EventBus;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeDeleter {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    EventBus eventBus = mock(EventBus.class);
    ArgumentCaptor<Office> officeArgumentCaptor = ArgumentCaptor.forClass(Office.class);

    OfficeDeleter deleter = new OfficeDeleter(officeRepo, permissionValidator, eventBus);

    @Test
    void itShouldReturnOfficeNotFoundWhenOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<OfficeError, Void> response = deleter.delete(officeId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserHasNoPerms() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(false);

        Either<OfficeError, Void> response = deleter.delete(office.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_FORBIDDEN);
    }

    @Test
    void itShouldReturnDeleteOfficeTwoMonthInTheFuture() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE))
        )).thenReturn(true);
        when(officeRepo.update(any())).thenReturn(Try.success(null));

        Either<OfficeError, Void> response = deleter.delete(office.id());

        assertThat(response.isRight()).isTrue();
        verify(officeRepo, times(1)).update(officeArgumentCaptor.capture());
        var officeDeleted = officeArgumentCaptor.getValue();
        assertThat(officeDeleted.isDeleted(LocalDate.now(Clock.systemUTC()).plusMonths(2).plusDays(1))).isTrue();
        assertThat(officeDeleted.isDeleted(LocalDate.now(Clock.systemUTC()).plusMonths(1))).isFalse();
        verify(eventBus, times(1)).publish(OfficeDeletedEvent.of(
                office.officeBranch().id().toString(),
                office.id().toString()
        ));
    }
}
