package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.application.dto.collaborator.CollaboratorUpdateInformation;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleRepository;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCollaboratorUpdater {
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    RoleRepository roleRepo = mock(RoleRepository.class);
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    ArgumentCaptor<Collaborator> collaboratorArgumentCaptor = ArgumentCaptor.forClass(Collaborator.class);

    CollaboratorUpdater updater = new CollaboratorUpdater(permissionValidator, roleRepo, collaboratorRepo);

    @Test
    void itShouldReturnCollaboratorNotFoundWhenThereIsNoCollaboratorWithIdSpecified() {
        var collaboratorId = new CollaboratorId();
        when(collaboratorRepo.findById(collaboratorId)).thenReturn(Option.none());
        var collaboratorUpdateInformation = CollaboratorUpdateInformation.of(
                "new name",
                Set.of(UUID.randomUUID())
        );

        Either<UseCaseError, Void> response = updater.update(collaboratorId, collaboratorUpdateInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToCollaboratorsRead() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.COLLABORATOR))
        )).thenReturn(false);
        var collaboratorUpdateInformation = CollaboratorUpdateInformation.of(
                "new name",
                Set.of(UUID.randomUUID())
        );

        Either<UseCaseError, Void> response = updater.update(collaborator.id(), collaboratorUpdateInformation);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.FORBIDDEN);
    }

    @Test
    void itShouldUpdateCollaboratorWithRolesRelatedToOfficeBranch() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.COLLABORATOR))
        )).thenReturn(true);
        when(collaboratorRepo.update(any())).thenReturn(Try.success(null));
        var newRole1 = new RoleBuilder().build();
        var newRole2 = new RoleBuilder().build();
        var newRole3 = new RoleBuilder().build();
        when(roleRepo.findByOfficeBranch(any(OfficeBranch.class))).thenReturn(
                ImmutableList.of(newRole1, newRole2)
        );
        var collaboratorUpdateInformation = CollaboratorUpdateInformation.of(
                "new name",
                Set.of(
                        UUID.fromString(newRole1.id().toString()),
                        UUID.fromString(newRole2.id().toString()),
                        UUID.fromString(newRole3.id().toString())
                )
        );

        Either<UseCaseError, Void> response = updater.update(collaborator.id(), collaboratorUpdateInformation);

        assertThat(response.isRight()).isTrue();
        verify(collaboratorRepo, times(1)).update(collaboratorArgumentCaptor.capture());
        var collaboratorUpdated = collaboratorArgumentCaptor.getValue();
        assertThat(collaboratorUpdated.id()).isEqualTo(collaborator.id());
        assertThat(collaboratorUpdated.name()).isEqualTo("new name");
        assertThat(collaboratorUpdated.roles()).size().isEqualTo(2);
        assertThat(collaboratorUpdated.roles()).map(Role::id).containsExactlyInAnyOrder(
                newRole1.id(),
                newRole2.id()
        );
    }
}
