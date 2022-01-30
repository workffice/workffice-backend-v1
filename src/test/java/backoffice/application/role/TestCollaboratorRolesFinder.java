package backoffice.application.role;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.dto.role.RoleResponse;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.RoleBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCollaboratorRolesFinder {
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);

    CollaboratorRolesFinder finder = new CollaboratorRolesFinder(permissionValidator, collaboratorRepo);

    @Test
    void itShouldReturnCollaboratorNotFoundWhenThereIsNoCollaboratorWithIdSpecified() {
        var collaboratorId = new CollaboratorId();
        when(collaboratorRepo.findWithRoles(collaboratorId)).thenReturn(Option.none());

        Either<UseCaseError, List<RoleResponse>> response = finder.find(collaboratorId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToOfficeBranchCollaboratorRoles() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findWithRoles(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.READ, Resource.COLLABORATOR))
        )).thenReturn(false);

        Either<UseCaseError, List<RoleResponse>> response = finder.find(collaborator.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldReturnRolesRelatedToCollaborator() {
        var role1 = new RoleBuilder().build();
        var role2 = new RoleBuilder().build();
        var collaborator = new CollaboratorBuilder()
                .addRole(role1)
                .addRole(role2)
                .build();
        when(collaboratorRepo.findWithRoles(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.READ, Resource.COLLABORATOR))
        )).thenReturn(true);

        Either<UseCaseError, List<RoleResponse>> response = finder.find(collaborator.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).size().isEqualTo(2);
        assertThat(response.get()).containsExactlyInAnyOrder(
                role1.toResponse(),
                role2.toResponse()
        );
    }
}
