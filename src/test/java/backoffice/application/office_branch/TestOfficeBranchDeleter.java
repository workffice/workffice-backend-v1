package backoffice.application.office_branch;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office.OfficesFinder;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchDeletedEvent;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeBuilder;
import com.google.inject.internal.util.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.domain.EventBus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestOfficeBranchDeleter {
    EventBus eventBus = mock(EventBus.class);
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeBranchAuthValidator officeBranchAuthValidator = mock(OfficeBranchAuthValidator.class);
    OfficesFinder officesFinder = mock(OfficesFinder.class);
    ArgumentCaptor<OfficeBranch> officeBranchArgumentCaptor = ArgumentCaptor.forClass(OfficeBranch.class);

    OfficeBranchDeleter deleter = new OfficeBranchDeleter(eventBus, officeBranchRepo, officeBranchAuthValidator,
            officesFinder);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoOfficeBranchWithIdProvided() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchRepo.findById(officeBranchId)).thenReturn(Option.none());

        Either<OfficeBranchError, Void> response = deleter.delete(officeBranchId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserIsNotOwnerOfOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(false);

        Either<OfficeBranchError, Void> response = deleter.delete(officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnErrorWhenOfficeBranchHasCreatedOffices() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(true);
        var office1 = new OfficeBuilder().build();
        when(officesFinder.find(officeBranch.id())).thenReturn(
                Either.right(ImmutableList.of(office1.toResponse()))
        );

        Either<OfficeBranchError, Void> response = deleter.delete(officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_HAS_CREATED_OFFICES);
    }

    @Test
    void itShouldDeleteOfficeBranchAndPublishEvent() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(true);
        when(officesFinder.find(officeBranch.id())).thenReturn(
                Either.right(ImmutableList.of())
        );
        when(officeBranchRepo.update(any())).thenReturn(Try.success(null));

        Either<OfficeBranchError, Void> response = deleter.delete(officeBranch.id());

        assertThat(response.isRight()).isTrue();
        verify(officeBranchRepo, times(1)).update(officeBranchArgumentCaptor.capture());
        var officeBranchDeleted = officeBranchArgumentCaptor.getValue();
        assertThat(officeBranchDeleted.isDeleted()).isTrue();
        verify(eventBus, times(1)).publish(OfficeBranchDeletedEvent.of(officeBranch.id().toString()));
    }
}
