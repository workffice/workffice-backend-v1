package backoffice.application.office_branch;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_branch.OfficeBranchUpdatedEvent;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableList;
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

public class TestOfficeBranchUpdater {
    EventBus eventBus = mock(EventBus.class);
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeBranchAuthValidator officeBranchAuthValidator = mock(OfficeBranchAuthValidator.class);
    ArgumentCaptor<OfficeBranch> officeBranchArgumentCaptor = ArgumentCaptor.forClass(OfficeBranch.class);
    OfficeBranchUpdateInformation info = OfficeBranchUpdateInformation.of(
            "Updated name",
            "Some desc",
            "1234",
            ImmutableList.of("imageurl.com"),
            "Mendoza",
            "Lujan",
            "calle falsa",
            "5501"
    );

    OfficeBranchUpdater updater = new OfficeBranchUpdater(eventBus, officeBranchRepo, officeBranchAuthValidator);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenThereIsNoOfficeBranchWithSpecifiedId() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchRepo.findById(officeBranchId)).thenReturn(Option.none());

        Either<OfficeBranchError, Void> response = updater.update(officeBranchId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUseIsNotTheOwner() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(false);

        Either<OfficeBranchError, Void> response = updater.update(officeBranch.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldUpdateOfficeBranchWithSpecifiedFields() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(true);
        when(officeBranchRepo.update(any(OfficeBranch.class))).thenReturn(Try.success(null));

        Either<OfficeBranchError, Void> response = updater.update(officeBranch.id(), info);

        assertThat(response.isRight()).isTrue();
        verify(officeBranchRepo, times(1))
                .update(officeBranchArgumentCaptor.capture());
        var officeBranchUpdated = officeBranchArgumentCaptor.getValue();
        assertThat(officeBranchUpdated.name()).isEqualTo(info.name().get());
        assertThat(officeBranchUpdated.description()).isEqualTo(info.description().get());
        assertThat(officeBranchUpdated.phone()).isEqualTo(info.phone().get());
        assertThat(officeBranchUpdated.images()).containsExactlyInAnyOrder(
                new Image("imageurl.com")
        );
        assertThat(officeBranchUpdated.location().province()).isEqualTo(info.province().get());
        assertThat(officeBranchUpdated.location().city()).isEqualTo(info.city().get());
        assertThat(officeBranchUpdated.location().street()).isEqualTo(info.street().get());
        assertThat(officeBranchUpdated.location().zipCode()).isEqualTo(info.zipCode().get());
    }

    @Test
    void itShouldDispatchOfficeBranchUpdatedEventWhenOfficeBranchIsUpdatedSuccessfully() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(officeBranchAuthValidator.authUserIsOwner(any(OfficeBranch.class))).thenReturn(true);
        when(officeBranchRepo.update(any(OfficeBranch.class))).thenReturn(Try.success(null));

        Either<OfficeBranchError, Void> response = updater.update(officeBranch.id(), info);

        assertThat(response.isRight()).isTrue();

        verify(eventBus, times(1)).publish(OfficeBranchUpdatedEvent.of(
                officeBranch.id().toString(),
                info.getName(),
                info.getProvince(),
                info.getCity(),
                info.getStreet(),
                info.getPhone(),
                info.getImagesUrls()
        ));
    }
}
