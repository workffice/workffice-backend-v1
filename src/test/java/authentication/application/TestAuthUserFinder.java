package authentication.application;

import authentication.application.dto.user.AuthUserResponse;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserRepository;
import authentication.factories.AuthUserBuilder;
import backoffice.application.UserTypeResolver;
import io.vavr.control.Option;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static backoffice.application.UserTypeResolver.UserType.COLLABORATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAuthUserFinder {

    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    AuthUserRepository repository = mock(AuthUserRepository.class);
    UserTypeResolver userTypeResolver = mock(UserTypeResolver.class);
    AuthUserFinder finder = new AuthUserFinder(repository, tokenGenerator, userTypeResolver);

    @Test
    public void itShouldReturnEmptyAuthUserWhenTokenGeneratorCannotParseToken() {
        when(tokenGenerator.parseToken(any(Token.class))).thenReturn(Option.none());

        Option<AuthUser> authUser = finder.find(new Token("super_token"));

        assertThat(authUser.isEmpty()).isTrue();
    }

    @Test
    public void itShouldReturnAuthUserWithTheIdParsedBtTokenGenerator() {
        AuthUser authUser = new AuthUserBuilder()
                .withEmail("test@mail.com")
                .withPassword("1234").build();
        when(tokenGenerator.parseToken(any(Token.class))).thenReturn(Option.of(authUser.id()));
        when(repository.findById(authUser.id())).thenReturn(Option.of(authUser));

        AuthUser authUserFound = finder.find(new Token("super_token")).get();

        assertThat(authUserFound.email()).isEqualTo("test@mail.com");
        assertThat(authUserFound.password()).isEqualTo("1234");
    }

    @Test
    void itShouldReturnEmptyWhenThereIsNoAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        Option<AuthUserResponse> authUserResponse = finder.findAuthenticatedUser();

        assertThat(authUserResponse.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnAuthUserEmailAndIdForAuthenticatedUser() {
        AuthUser authUser = new AuthUserBuilder().build();
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        new User(authUser.email(), authUser.password(), new ArrayList<>()),
                        ""
                ));
        when(repository.findByEmail(authUser.email())).thenReturn(Option.of(authUser));
        when(userTypeResolver.getUserType(authUser.email())).thenReturn(COLLABORATOR);

        Option<AuthUserResponse> authUserResponse = finder.findAuthenticatedUser();

        assertThat(authUserResponse.isDefined()).isTrue();
        assertThat(authUserResponse.get()).isEqualTo(authUser.toResponse("COLLABORATOR"));
    }
}
