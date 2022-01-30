package authentication.infrastructure;

import authentication.domain.token.Token;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.factories.AuthUserBuilder;
import authentication.infrastructure.springsecurity.JwtTokenGenerator;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestJwtTokenGenerator {

    @Autowired
    JwtTokenGenerator jwtTokenGenerator;

    @Test
    public void itShouldReturnEmptyAuthUserIdWhenTokenHasExpired() {
        JwtParser parser = mock(JwtParser.class);
        when(parser.parseClaimsJws("super_token")).thenThrow(ExpiredJwtException.class);

        Option<AuthUserId> id = jwtTokenGenerator.parseToken(new Token("super_token"));

        assertThat(id.isEmpty()).isTrue();
    }

    @Test
    public void itShouldReturnEmptyAuthUserIdWhenTokenIsInvalid() {
        JwtParser parser = mock(JwtParser.class);
        when(parser.parseClaimsJws("super_token")).thenThrow(MalformedJwtException.class);

        Option<AuthUserId> id = jwtTokenGenerator.parseToken(new Token("super_token"));

        assertThat(id.isEmpty()).isTrue();
    }

    @Test
    public void itShouldReturnAuthUserIdEncryptedInToken() {
        AuthUserId id = new AuthUserId();
        AuthUser authUser = new AuthUserBuilder().withId(id).build();

        Token token = jwtTokenGenerator.create(authUser);
        AuthUserId idDecrypted = jwtTokenGenerator.parseToken(token).get();

        assertThat(idDecrypted).isEqualTo(id);
    }
}
