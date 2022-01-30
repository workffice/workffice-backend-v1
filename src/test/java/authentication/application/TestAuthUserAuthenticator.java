package authentication.application;

import authentication.application.dto.token.Authentication;
import authentication.application.dto.user.UserError;
import authentication.application.dto.user.UserLoginInformation;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.token.TokenRepository;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.PasswordEncoder;
import authentication.domain.user.Status;
import authentication.factories.AuthUserBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestAuthUserAuthenticator {

    AuthUserRepository authUserRepository = mock(AuthUserRepository.class);
    TokenRepository tokenRepository = mock(TokenRepository.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    AuthUserAuthenticator authenticator = new AuthUserAuthenticator(
            authUserRepository,
            tokenRepository,
            passwordEncoder,
            tokenGenerator
    );

    @Test
    void itShouldReturnInvalidTokenWhenPasswordDoesNotMatch() {
        AuthUser authUser = new AuthUserBuilder().withEmail("test@mail.com").build();
        when(authUserRepository.findByEmail(anyString())).thenReturn(Option.of(authUser));
        when(passwordEncoder.match(anyString(), anyString())).thenReturn(false);

        Either<UseCaseError, Authentication> authentication = authenticator
                .login(new UserLoginInformation("test@mail.com", "invalid_pass"));

        assertThat(authentication.isLeft()).isTrue();
        assertThat(authentication.getLeft()).isEqualTo(UserError.INVALID_PASSWORD);
    }

    @Test
    void itShouldReturnUserNotFoundWhenThereIsNoUserWithEmailProvided() {
        when(authUserRepository.findByEmail(anyString())).thenReturn(Option.none());

        Either<UseCaseError, Authentication> authentication = authenticator
                .login(new UserLoginInformation("test@mail.com", "12"));

        assertThat(authentication.isLeft()).isTrue();
        assertThat(authentication.getLeft()).isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void itShouldReturnTokenWhenUserExistsAndPasswordMatch() {
        AuthUser authUser = new AuthUserBuilder().build();
        when(authUserRepository.findByEmail(anyString())).thenReturn(Option.of(authUser));
        when(passwordEncoder.match(anyString(), anyString())).thenReturn(true);
        when(tokenGenerator.create(authUser)).thenReturn(new Token("super_token"));

        Either<UseCaseError, Authentication> authentication = authenticator
                .login(new UserLoginInformation("test@mail.com", "12"));

        assertThat(authentication.isRight()).isTrue();
        assertThat(authentication.get().getToken()).isEqualTo("super_token");
    }

    @Test
    void itShouldReturnNonActiveUserWhenUserIsNotActive() {
        AuthUserAuthenticator authenticator = new AuthUserAuthenticator(
                authUserRepository,
                tokenRepository,
                passwordEncoder,
                tokenGenerator
        );
        AuthUser authUser = new AuthUserBuilder().withStatus(Status.PENDING).build();
        when(authUserRepository.findByEmail(anyString())).thenReturn(Option.of(authUser));
        when(passwordEncoder.match(anyString(), anyString())).thenReturn(true);

        Either<UseCaseError, Authentication> authentication = authenticator
                .login(new UserLoginInformation("test@mail.com", "12"));

        assertThat(authentication.getLeft()).isEqualTo(UserError.NON_ACTIVE_USER);
    }

    @Test
    void itShouldStoreTokenWhenCallLogout() {
        Token token = new Token("a123");

        authenticator.logout(token);

        verify(tokenRepository, times(1)).store(token);
    }
}
