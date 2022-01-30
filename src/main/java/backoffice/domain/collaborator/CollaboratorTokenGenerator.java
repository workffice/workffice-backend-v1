package backoffice.domain.collaborator;

import io.vavr.control.Option;

public interface CollaboratorTokenGenerator {
    
    CollaboratorToken createToken(Collaborator collaborator);

    Option<CollaboratorId> parseToken(CollaboratorToken collaboratorToken);
}
