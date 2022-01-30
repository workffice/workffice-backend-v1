package backoffice.infrastructure;

import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.factories.CollaboratorBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.vavr.control.Option;
import server.WorkfficeApplication;
import shared.infrastructure.jwt.JwtGenerator;
import shared.infrastructure.jwt.JwtParser;
import shared.infrastructure.jwt.JwtToken;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestCollaboratorJwtTokenGenerator {
    @Autowired
    CollaboratorJwtTokenGenerator tokenGenerator;
    @Autowired
    JwtParser jwtParser;

    @Test
    void itShouldReturnCollaboratorTokenWithIdSpecified() {
        var collaborator = new CollaboratorBuilder().build();

        CollaboratorToken token = tokenGenerator.createToken(collaborator);

        String collaboratorId =
                jwtParser.parseToken(JwtToken.of(token.getToken()), "collaborator_id", String.class).get();
        assertThat(collaboratorId).isEqualTo(collaborator.id().toString());
    }

    @Test
    void itShouldReturnEmptyTokenWhenJwtGeneratorReturnsEmptyToken() {
        JwtGenerator generator = mock(JwtGenerator.class);
        JwtParser parser = mock(JwtParser.class);
        var collaboratorTokenGenerator = new CollaboratorJwtTokenGenerator(generator, parser);
        when(generator.generate(any())).thenReturn(Option.none());

        CollaboratorToken token = collaboratorTokenGenerator.createToken(new CollaboratorBuilder().build());

        assertThat(token.getToken()).isEmpty();
    }

    @Test
    void itShouldReturnEmptyCollaboratorIdWhenTokenIsInvalid() {
        io.jsonwebtoken.JwtParser jwtParser = mock(io.jsonwebtoken.JwtParser.class);
        when(jwtParser.parse("some")).thenThrow(MalformedJwtException.class);

        Option<CollaboratorId> collaboratorId = tokenGenerator.parseToken(CollaboratorToken.of("some"));

        assertThat(collaboratorId.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnEmptyCollaboratorIdWhenTokenHasExpired() {
        io.jsonwebtoken.JwtParser jwtParser = mock(io.jsonwebtoken.JwtParser.class);
        when(jwtParser.parse("some")).thenThrow(ExpiredJwtException.class);

        Option<CollaboratorId> collaboratorId = tokenGenerator.parseToken(CollaboratorToken.of("some"));

        assertThat(collaboratorId.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnCollaboratorIdFromToken() {
        var collaborator = new CollaboratorBuilder().build();
        var collaboratorToken = tokenGenerator.createToken(collaborator);

        Option<CollaboratorId> collaboratorId = tokenGenerator.parseToken(collaboratorToken);

        assertThat(collaboratorId.get()).isEqualTo(collaborator.id());
    }
}
