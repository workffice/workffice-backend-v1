package backoffice.application.office_branch;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.office_branch.OfficeBranchResponse;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOfficeBranchFinderWithAuth {
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeHolderRepository officeHolderRepo = mock(OfficeHolderRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    OfficeBranchFinder finder = new OfficeBranchFinder(
            permissionValidator,
            officeBranchRepo,
            officeHolderRepo
    );

    @Test
    void itShouldReturnNotFoundWhenOfficeBranchDoesNotExistWhenAskForPerm() {
        OfficeBranchId id = new OfficeBranchId();
        when(officeBranchRepo.findById(id)).thenReturn(Option.none());

        Either<UseCaseError, OfficeBranchResponse> response = finder.findWithAuthorization(
                id,
                Permission.create(Access.READ, Resource.ROLE)
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserHasNoPermsForOfficeBranch() {
        OfficeBranch officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.ROLE);
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(permissionValidator.userHasPerms(any(OfficeBranch.class), eq(permission))).thenReturn(false);

        Either<UseCaseError, OfficeBranchResponse> response = finder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.ROLE)
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }


    @Test
    void itShouldReturnOfficeBranchDataWhenAuthUserHasPerms() {
        var images = Arrays.asList(new Image("imageurl"), new Image("image2url"));
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withImages(images)
                .build();
        var permission = Permission.create(Access.WRITE, Resource.OFFICE);
        when(officeBranchRepo.findById(officeBranch.id())).thenReturn(Option.of(officeBranch));
        when(permissionValidator.userHasPerms(any(OfficeBranch.class), eq(permission))).thenReturn(true);

        Either<UseCaseError, OfficeBranchResponse> response = finder.findWithAuthorization(
                officeBranch.id(),
                permission
        );

        var officeBranchResponse = response.get();
        assertThat(officeBranchResponse).isEqualTo(officeBranch.toResponse());
    }
}
