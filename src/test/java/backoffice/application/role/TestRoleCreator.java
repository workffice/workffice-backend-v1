package backoffice.application.role;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.role.RoleInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestRoleCreator {
    RoleRepository roleRepo = mock(RoleRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    ArgumentCaptor<Role> roleArgumentCaptor = ArgumentCaptor.forClass(Role.class);

    RoleCreator roleCreator = new RoleCreator(roleRepo, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenThereIsNoOfficeBranchWithIdSpecified() {
        var roleInformation = RoleInformation.of(
                "Awesome role",
                Collections.singletonList(new RoleInformation.Permission("OFFICE", "WRITE"))
        );
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.ROLE)))
                .thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        var response = roleCreator
                .createRole(officeBranchId, new RoleId(), roleInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserHasPermissionToOfficeBranch() {
        var roleInformation = RoleInformation.of(
                "Awesome role",
                Collections.singletonList(new RoleInformation.Permission("OFFICE", "WRITE"))
        );
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.ROLE))
        ).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        var response = roleCreator
                .createRole(officeBranch.id(), new RoleId(), roleInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldCallRoleRepositoryWithPermissionsSpecified() {
        var roleInformation = RoleInformation.of(
                "Awesome role",
                Collections.singletonList(new RoleInformation.Permission("OFFICE", "WRITE"))
        );
        var officeBranch = new OfficeBranchBuilder().build();
        RoleId roleId = new RoleId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.ROLE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(roleRepo.store(any())).thenReturn(Try.success(null));

        var response = roleCreator
                .createRole(officeBranch.id(), roleId, roleInformation);

        assertThat(response.isRight()).isTrue();
        verify(roleRepo, times(1)).store(roleArgumentCaptor.capture());
        Role roleStored = roleArgumentCaptor.getValue();
        assertThat(roleStored.id()).isEqualTo(roleId);
        assertThat(roleStored.name()).isEqualTo("Awesome role");
        assertThat(roleStored.permissions())
                .isEqualTo(Set.of(Permission.create(Access.WRITE, Resource.OFFICE)));
    }
}
