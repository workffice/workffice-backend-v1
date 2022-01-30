package backoffice.application;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.AuthUserResponse;
import backoffice.application.office_branch.OfficeBranchAuthValidator;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.collaborator.Status;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPermissionValidator {
    AuthUserFinder authUserFinder = mock(AuthUserFinder.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    OfficeBranchAuthValidator officeBranchAuthValidator = mock(OfficeBranchAuthValidator.class);

    AuthUserResponse authUserResponse = AuthUserResponse.of(
            "1",
            "wick@john.com",
            "john",
            "doe",
            "",
            "",
            "",
            "image.url"
    );
    PermissionValidator permissionValidator = new PermissionValidator(
            authUserFinder,
            collaboratorRepo,
            officeBranchAuthValidator
    );

    @Test
    void itShouldReturnFalseWhenThereIsNoAuthenticatedUser() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.none());

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenAuthUserIsCollaboratorWithoutPermissions() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        var role = new RoleBuilder()
                .withPermissions(ImmutableSet.of(Permission.create(Access.READ, Resource.ROLE)))
                .build();
        var collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .addRole(role).build();
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.of(collaborator));

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenAuthUserIsNotCollaboratorAndDoesNotOwnOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.none());
        when(officeBranchAuthValidator.authUserIsOwner(any())).thenReturn(false);

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenAuthUserIsNotActiveCollaborator() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        var role = new RoleBuilder()
                .withPermissions(ImmutableSet.of(permission))
                .build();
        var collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .withStatus(Status.PENDING)
                .addRole(role).build();
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.of(collaborator));

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenAuthUserIsActiveCollaboratorWithPermissionSpecified() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        var role = new RoleBuilder()
                .withPermissions(ImmutableSet.of(permission))
                .build();
        var collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .withStatus(Status.ACTIVE)
                .addRole(role).build();
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.of(collaborator));

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenCollaboratorHasWriteAccessToResourceAndAskForReadPermission() {
        var officeBranch = new OfficeBranchBuilder().build();
        var readPermission = Permission.create(Access.READ, Resource.OFFICE);
        var writePermission = Permission.create(Access.WRITE, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        var role = new RoleBuilder()
                .withPermissions(ImmutableSet.of(writePermission))
                .build();
        var collaborator = new CollaboratorBuilder()
                .withOfficeBranch(officeBranch)
                .withStatus(Status.ACTIVE)
                .addRole(role).build();
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.of(collaborator));

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, readPermission);

        assertThat(hasPermission).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenAuthUserIsOwnerOfOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permission = Permission.create(Access.READ, Resource.OFFICE);
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserResponse));
        when(collaboratorRepo.find(eq("wick@john.com"), any(OfficeBranch.class)))
                .thenReturn(Option.none());
        when(officeBranchAuthValidator.authUserIsOwner(any())).thenReturn(true);

        boolean hasPermission = permissionValidator.userHasPerms(officeBranch, permission);

        assertThat(hasPermission).isTrue();
    }
}
