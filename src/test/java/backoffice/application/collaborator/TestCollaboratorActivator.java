package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.domain.collaborator.CollaboratorTokenGenerator;
import backoffice.domain.collaborator.Status;
import backoffice.factories.CollaboratorBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCollaboratorActivator {

    CollaboratorRepository collaboratorRepo = mock(CollaboratorRepository.class);
    CollaboratorTokenGenerator tokenGenerator = mock(CollaboratorTokenGenerator.class);
    ArgumentCaptor<Collaborator> collaboratorArgumentCaptor = ArgumentCaptor.forClass(Collaborator.class);

    CollaboratorActivator collaboratorActivator = new CollaboratorActivator(collaboratorRepo, tokenGenerator);

    @Test
    void itShouldReturnInvalidTokenWhenParseFails() {
        var collaboratorToken = CollaboratorToken.of("some");
        when(tokenGenerator.parseToken(collaboratorToken)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = collaboratorActivator.activate(collaboratorToken);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.INVALID_TOKEN);
    }

    @Test
    void itShouldReturnCollaboratorNotFoundWhenIdProvidedByTokenDoesNotExist() {
        var collaboratorToken = CollaboratorToken.of("some");
        var collaboratorId = new CollaboratorId();
        when(tokenGenerator.parseToken(collaboratorToken)).thenReturn(Option.of(collaboratorId));
        when(collaboratorRepo.findById(collaboratorId)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = collaboratorActivator.activate(collaboratorToken);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(CollaboratorError.COLLABORATOR_NOT_FOUND);
    }

    @Test
    void itShouldUpdateCollaboratorStatus() {
        var collaborator = new CollaboratorBuilder()
                .withStatus(Status.PENDING)
                .build();
        var collaboratorToken = CollaboratorToken.of("some");
        when(tokenGenerator.parseToken(collaboratorToken)).thenReturn(Option.of(collaborator.id()));
        when(collaboratorRepo.findById(collaborator.id())).thenReturn(Option.of(collaborator));
        when(collaboratorRepo.update(collaborator)).thenReturn(Try.success(null));

        Either<UseCaseError, Void> response = collaboratorActivator.activate(collaboratorToken);

        assertThat(response.isRight()).isTrue();
        verify(collaboratorRepo, times(1)).update(collaboratorArgumentCaptor.capture());
        var collaboratorUpdated = collaboratorArgumentCaptor.getValue();
        assertThat(collaboratorUpdated.isActive()).isTrue();

    }
}
