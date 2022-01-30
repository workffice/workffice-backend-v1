package authentication.application;

import authentication.application.dto.token.TokenError;
import authentication.application.dto.user.UserError;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.token.TokenRepository;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.Status;
import authentication.factories.AuthUserBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestAuthUserActivator {

    TokenRepository mockTokenRepository = mock(TokenRepository.class);
    AuthUserRepository mockAuthUserRepository = mock(AuthUserRepository.class);
    TokenGenerator mockTokenGenerator = mock(TokenGenerator.class);

    @Test
    void itShouldReturnInvalidTokenWhenTokenCouldNotBeParsed() {
        AuthUserActivator activator = new AuthUserActivator(
                mockTokenRepository,
                mockAuthUserRepository,
                mockTokenGenerator
        );
        Token fakeToken = new Token("1234");
        when(mockTokenRepository.find("1234")).thenReturn(Option.none());
        when(mockTokenGenerator.parseToken(fakeToken)).thenReturn(Option.none());

        Either<UseCaseError, AuthUser> response = activator.activateUser(fakeToken);

        assertThat(response.getLeft()).isEqualTo(TokenError.INVALID_TOKEN);
    }

    @Test
    void itShouldReturnTokenAlreadyUsedWhenTokenIsInTheBlockList() {
        AuthUserActivator activator = new AuthUserActivator(
                mockTokenRepository,
                mockAuthUserRepository,
                mockTokenGenerator
        );
        Token fakeToken = new Token("1234");
        when(mockTokenRepository.find("1234")).thenReturn(Option.of(fakeToken));

        Either<UseCaseError, AuthUser> response = activator.activateUser(fakeToken);

        assertThat(response.getLeft()).isEqualTo(TokenError.TOKEN_ALREADY_USED);
    }

    @Test
    void itShouldReturnUserNotFoundWhenUserDoesNotExist() {
        AuthUserActivator activator = new AuthUserActivator(
                mockTokenRepository,
                mockAuthUserRepository,
                mockTokenGenerator
        );
        Token fakeToken = new Token("1234");
        AuthUserId fakeAuthUserId = new AuthUserId();
        when(mockTokenRepository.find("1234")).thenReturn(Option.none());
        when(mockTokenGenerator.parseToken(fakeToken)).thenReturn(Option.of(fakeAuthUserId));
        when(mockAuthUserRepository.findById(fakeAuthUserId)).thenReturn(Option.none());

        Either<UseCaseError, AuthUser> response = activator.activateUser(fakeToken);

        assertThat(response.getLeft()).isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void itShouldReturnUserActivated() {
        AuthUserActivator activator = new AuthUserActivator(
                mockTokenRepository,
                mockAuthUserRepository,
                mockTokenGenerator
        );
        Token fakeToken = new Token("1234");
        AuthUser inactiveUser = new AuthUserBuilder().withStatus(Status.PENDING).build();
        when(mockTokenRepository.find("1234")).thenReturn(Option.none());
        when(mockTokenGenerator.parseToken(fakeToken)).thenReturn(Option.of(inactiveUser.id()));
        when(mockAuthUserRepository.findById(inactiveUser.id())).thenReturn(Option.of(inactiveUser));

        Either<UseCaseError, AuthUser> response = activator.activateUser(fakeToken);

        verify(mockAuthUserRepository, times(1)).update(any(AuthUser.class));
        assertThat(response.get().isActive()).isTrue();
    }
}
