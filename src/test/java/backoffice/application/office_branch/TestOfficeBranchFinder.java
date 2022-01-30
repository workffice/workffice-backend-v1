package backoffice.application.office_branch;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Option;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchFinder {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeHolderRepository officeHolderRepo = mock(OfficeHolderRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    OfficeBranchFinder finder = new OfficeBranchFinder(
            permissionValidator,
            officeBranchRepo,
            officeHolderRepo
    );

    @Test
    void itShouldReturnEmptyOfficeBranchWhenThereIsNoOfficeBranchWithIdSpecified() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchRepo.findById(officeBranchId)).thenReturn(Option.none());

        Option<OfficeBranchResponse> officeBranchResponse = finder.find(officeBranchId);

        assertThat(officeBranchResponse.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnOfficeBranchWithIdSpecified() {
        var images = Arrays.asList(new Image("imageurl"), new Image("image2url"));
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withImages(images)
                .build();
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));

        Option<OfficeBranchResponse> officeBranchResponse = finder.find(officeBranch.id());

        assertThat(officeBranchResponse.isDefined()).isTrue();
        assertThat(officeBranchResponse.get()).isEqualTo(officeBranch.toResponse());
    }
}
