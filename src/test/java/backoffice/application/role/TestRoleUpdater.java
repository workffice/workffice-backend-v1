package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.role.RoleError;
import backoffice.application.dto.role.RoleInformation;
import backoffice.application.dto.role.RoleResponse;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.Sets;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestRoleUpdater {

    RoleRepository roleRepo = mock(RoleRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    RoleUpdater updater = new RoleUpdater(roleRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenRoleDoesNotExist() {
        var roleInformation = RoleInformation.of("New name", Lists.emptyList());
        var roleId = new RoleId();
        when(roleRepo.findById(roleId)).thenReturn(Option.none());

        Either<UseCaseError, RoleResponse> response = updater.update(roleId, roleInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(RoleError.ROLE_NOT_FOUND);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenAuthUserHasNoAccessToOfficeBranchRole() {
        var roleInformation = RoleInformation.of("New name", Lists.emptyList());
        var role = new RoleBuilder().build();
        when(roleRepo.findById(role.id())).thenReturn(Option.of(role));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.ROLE))
        )).thenReturn(false);

        Either<UseCaseError, RoleResponse> response = updater.update(role.id(), roleInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(RoleError.ROLE_FORBIDDEN);
    }

    @Test
    void itShouldCallUpdateAndReturnRoleResponseWithUpdatedFields() {
        var roleInformation = RoleInformation.of(
                "New name",
                Lists.newArrayList(new RoleInformation.Permission("ROLE", "READ"))
        );
        var role = new RoleBuilder().build();
        when(roleRepo.findById(role.id())).thenReturn(Option.of(role));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.ROLE))
        )).thenReturn(true);
        when(roleRepo.update(any())).thenReturn(Try.success(null));

        Either<UseCaseError, RoleResponse> response = updater.update(role.id(), roleInformation);

        verify(roleRepo, times(1))
                .update(Role.create(
                        role.id(),
                        "New name",
                        Sets.newHashSet(Permission.create(Access.READ, Resource.ROLE)),
                        role.officeBranch()
                ));
        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(RoleResponse.of(
                role.id().toString(),
                "New name",
                Sets.newHashSet(RoleResponse.PermissionResponse.of("ROLE", "READ"))
        ));
    }
}
