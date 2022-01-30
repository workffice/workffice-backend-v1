package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.role.RoleError;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.RoleBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestRoleDeleter {
    RoleRepository roleRepo = mock(RoleRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    ArgumentCaptor<Role> roleArgumentCaptor = ArgumentCaptor.forClass(Role.class);

    RoleDeleter deleter = new RoleDeleter(roleRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoRoleWithIdSpecified() {
        var roleId = new RoleId();
        when(roleRepo.findById(roleId)).thenReturn(Option.none());

        Either<RoleError, Void> response = deleter.delete(roleId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(RoleError.ROLE_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToRole() {
        var role = new RoleBuilder().build();
        when(roleRepo.findById(role.id())).thenReturn(Option.of(role));
        when(permissionValidator.userHasPerms(any(), eq(Permission.create(Access.WRITE, Resource.ROLE))))
                .thenReturn(false);

        Either<RoleError, Void> response = deleter.delete(role.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(RoleError.ROLE_FORBIDDEN);
    }

    @Test
    void itShouldUpdateRoleWithDeletedTrue() {
        var role = new RoleBuilder().build();
        when(roleRepo.findById(role.id())).thenReturn(Option.of(role));
        when(permissionValidator.userHasPerms(any(), eq(Permission.create(Access.WRITE, Resource.ROLE))))
                .thenReturn(true);
        when(roleRepo.update(any())).thenReturn(Try.success(null));

        Either<RoleError, Void> response = deleter.delete(role.id());

        assertThat(response.isRight()).isTrue();
        verify(roleRepo, times(1)).update(roleArgumentCaptor.capture());
        var roleUpdated = roleArgumentCaptor.getValue();
        assertThat(roleUpdated.isActive()).isFalse();
    }
}
