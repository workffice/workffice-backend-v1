package backoffice.application.role;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.role.RoleResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.RoleBuilder;
import io.vavr.control.Either;

import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRoleFinder {
    RoleRepository roleRepo = mock(RoleRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    RoleResponse roleResponse(Role role) {
        var permissions = role.permissions()
                .stream()
                .map(perm -> RoleResponse.PermissionResponse.of(perm.resource().name(), perm.access().name()))
                .collect(Collectors.toSet());
        return RoleResponse.of(
                role.id().toString(),
                role.name(),
                permissions
        );
    }

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchDoesNotExist() {
        var officeBranch = new OfficeBranchBuilder().build();
        var finder = new RoleFinder(roleRepo, officeBranchFinder);
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.ROLE))
        ).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        var response = finder.findRoles(officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserDoesNotHaveAccess() {
        var officeBranch = new OfficeBranchBuilder().build();
        var finder = new RoleFinder(roleRepo, officeBranchFinder);
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.ROLE)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        var response = finder.findRoles(officeBranch.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnRolesRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var finder = new RoleFinder(roleRepo, officeBranchFinder);
        var role1 = new RoleBuilder().build();
        var role2 = new RoleBuilder().build();

        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.READ, Resource.ROLE)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(roleRepo.findByOfficeBranch(any()))
                .thenReturn(Lists.newArrayList(role1, role2));

        var response = finder.findRoles(officeBranch.id());

        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(
                roleResponse(role1),
                roleResponse(role2)
        );
    }
}
