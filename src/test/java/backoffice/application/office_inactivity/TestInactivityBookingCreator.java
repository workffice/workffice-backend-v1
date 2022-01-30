package backoffice.application.office_inactivity;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office.OfficeError;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_inactivity.InactivityError;
import backoffice.application.dto.office_inactivity.InactivityInformation;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityRepository;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBuilder;
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

public class TestInactivityBookingCreator {

    OfficeRepository officeRepo = mock(OfficeRepository.class);
    InactivityRepository inactivityRepo = mock(InactivityRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    EventBus eventBus = mock(EventBus.class);

    InactivityCreator creator = new InactivityCreator(
            officeRepo,
            inactivityRepo,
            permissionValidator,
            eventBus
    );

    @Test
    void itShouldReturnOfficeNotFoundWhenOfficeDoesNotExist() {
        var office = new OfficeBuilder().build();
        InactivityInformation info = InactivityInformation.of("SPECIFIC_DATE", null, LocalDate.now());
        when(officeRepo.findById(office.id())).thenReturn(Option.none());

        Either<UseCaseError, Void> response = creator.create(office.id(), new InactivityId(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserDoesNotHaveAccess() {
        var office = new OfficeBuilder().build();
        InactivityInformation info = InactivityInformation.of("SPECIFIC_DATE", null, LocalDate.now());
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE)))
        ).thenReturn(false);

        Either<UseCaseError, Void> response = creator.create(office.id(), new InactivityId(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnInactivityTypeMismatchWithDateWhenDateSpecifiedIsNotCompatibleForInactivityType() {
        var office = new OfficeBuilder().build();
        InactivityInformation info = InactivityInformation.of("SPECIFIC_DATE", DayOfWeek.FRIDAY, null);
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE)))
        ).thenReturn(true);

        Either<UseCaseError, Void> response = creator.create(office.id(), new InactivityId(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(InactivityError.INACTIVITY_TYPE_MISMATCH_WITH_DATE);
    }

    @Test
    void itShouldStoreInactivity() {
        var office = new OfficeBuilder().build();
        var inactivityId = new InactivityId();
        InactivityInformation info = InactivityInformation.of("RECURRING_DAY", DayOfWeek.FRIDAY, null);
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.OFFICE)))
        ).thenReturn(true);
        when(inactivityRepo.store(any())).thenReturn(Try.success(null));

        creator.create(office.id(), inactivityId, info);

        var expectedInactivityCreated = Inactivity.create(
                inactivityId,
                InactivityType.RECURRING_DAY,
                Option.of(DayOfWeek.FRIDAY),
                Option.none(),
                office
        ).get();
        verify(inactivityRepo, times(1)).store(expectedInactivityCreated);
        verify(eventBus, times(1)).publish(expectedInactivityCreated.inactivityCreatedEvent());
    }
}
