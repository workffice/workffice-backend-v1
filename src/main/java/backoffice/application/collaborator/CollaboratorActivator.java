package backoffice.application.collaborator;

import backoffice.application.dto.collaborator.CollaboratorError;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.domain.collaborator.CollaboratorTokenGenerator;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class CollaboratorActivator {

    private final CollaboratorRepository collaboratorRepo;
    private final CollaboratorTokenGenerator tokenGenerator;

    public CollaboratorActivator(CollaboratorRepository collaboratorRepo, CollaboratorTokenGenerator tokenGenerator) {
        this.collaboratorRepo = collaboratorRepo;
        this.tokenGenerator = tokenGenerator;
    }

    public Either<UseCaseError, Void> activate(CollaboratorToken collaboratorToken) {
        return tokenGenerator
                .parseToken(collaboratorToken)
                .toEither((UseCaseError) CollaboratorError.INVALID_TOKEN)
                .flatMap(id -> collaboratorRepo.findById(id).toEither(CollaboratorError.COLLABORATOR_NOT_FOUND))
                .flatMap(collaborator -> {
                    collaborator.activate();
                    return collaboratorRepo.update(collaborator).toEither(CollaboratorError.DB_ERROR);
                });

    }
}
