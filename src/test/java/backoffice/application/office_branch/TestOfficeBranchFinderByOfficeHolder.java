package backoffice.application.office_branch;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.application.dto.office_holder.OfficeHolderError;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeHolderBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchFinderByOfficeHolder {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeHolderRepository officeHolderRepo = mock(OfficeHolderRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    OfficeBranchFinder finder = new OfficeBranchFinder(
            permissionValidator,
            officeBranchRepo,
            officeHolderRepo
    );

    @Test
    void itShouldReturnOfficeHolderNotFoundWhenThereIsNoOfficeHolderWithSpecifiedId() {
        var officeHolderId = new OfficeHolderId();
        when(officeHolderRepo.findById(officeHolderId)).thenReturn(Option.none());

        Either<OfficeHolderError, List<OfficeBranchResponse>> response = finder.findByOfficeHolder(officeHolderId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeHolderError.OFFICE_HOLDER_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeBranchesRelatedWithOfficeHolder() {
        var officeHolder = new OfficeHolderBuilder().build();
        when(officeHolderRepo.findById(officeHolder.id())).thenReturn(Option.of(officeHolder));
        var officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .build();
        var officeBranch2 = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .build();
        var officeBranch3 = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .build();
        when(officeBranchRepo.findByOfficeHolder(any(OfficeHolder.class))).thenReturn(
                ImmutableList.of(officeBranch, officeBranch2, officeBranch3)
        );

        Either<OfficeHolderError, List<OfficeBranchResponse>> response = finder
                .findByOfficeHolder(officeHolder.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactlyInAnyOrder(
                officeBranch.toResponse(),
                officeBranch2.toResponse(),
                officeBranch3.toResponse()
        );
    }
}
