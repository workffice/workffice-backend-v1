package backoffice.infrastructure;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.domain.collaborator.CollaboratorTokenGenerator;
import io.vavr.control.Option;
import shared.infrastructure.jwt.JwtGenerator;
import shared.infrastructure.jwt.JwtParser;
import shared.infrastructure.jwt.JwtToken;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CollaboratorJwtTokenGenerator implements CollaboratorTokenGenerator {
    
    private final JwtGenerator generator;
    private final JwtParser parser;
    
    public CollaboratorJwtTokenGenerator(JwtGenerator generator, JwtParser parser) {
        this.generator = generator;
        this.parser = parser;
    }
    
    @Override
    public CollaboratorToken createToken(Collaborator collaborator) {
        Map<String, String> info = new HashMap<>() {{
            put("subject", collaborator.email());
            put("collaborator_id", collaborator.id().toString());
        }};
        return generator
                .generate(info)
                .map(jwtToken -> CollaboratorToken.of(jwtToken.getToken()))
                .getOrElse(CollaboratorToken.of(""));
    }

    @Override
    public Option<CollaboratorId> parseToken(CollaboratorToken collaboratorToken) {
        return parser
                .parseToken(JwtToken.of(collaboratorToken.getToken()), "collaborator_id", String.class)
                .map(CollaboratorId::fromString);
    }
}
