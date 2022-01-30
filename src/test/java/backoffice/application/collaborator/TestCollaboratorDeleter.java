package backoffice.application.collaborator;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
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

public class TestCollaboratorDeleter {
    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);
    ArgumentCaptor<Collaborator> collaboratorArgumentCaptor = ArgumentCaptor.forClass(Collaborator.class);

    CollaboratorDeleter deleter = new CollaboratorDeleter(collaboratorRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoCollaboratorWithIdSpecified() {
        var collaboratorId = new CollaboratorId();
        when(collaboratorRepo.findById(collaboratorId)).thenReturn(Option.none());

        Either<CollaboratorError, Void> response = deleter.delete(collaboratorId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToCollaborators() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.COLLABORATOR))
        )).thenReturn(false);

        Either<CollaboratorError, Void> response = deleter.delete(collaborator.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.FORBIDDEN);
    }

    @Test
    void itShouldUpdateCollaboratorWithStatusInactive() {
        var collaborator = new CollaboratorBuilder().build();
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(permissionValidator.userHasPerms(
                any(),
                eq(Permission.create(Access.WRITE, Resource.COLLABORATOR))
        )).thenReturn(true);
        when(collaboratorRepo.update(any())).thenReturn(Try.success(null));

        Either<CollaboratorError, Void> response = deleter.delete(collaborator.id());

        assertThat(response.isRight()).isTrue();
        verify(collaboratorRepo, times(1)).update(collaboratorArgumentCaptor.capture());
        var collaboratorUpdated = collaboratorArgumentCaptor.getValue();
        assertThat(collaboratorUpdated.isActive()).isFalse();
    }
}
