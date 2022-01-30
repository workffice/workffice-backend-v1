package authentication.application;

import authentication.application.dto.token.TokenError;
import authentication.application.dto.user.PasswordResetInformation;
import authentication.application.dto.user.UserError;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.token.TokenRepository;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.PasswordEncoder;
import authentication.factories.AuthUserBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestPasswordResetter {
    AuthUserRepository authUserRepo = mock(AuthUserRepository.class);
    TokenRepository tokenRepo = mock(TokenRepository.class);
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    ArgumentCaptor<AuthUser> authUserArgumentCaptor = ArgumentCaptor.forClass(AuthUser.class);

    PasswordResetter passwordResetter = new PasswordResetter(
            authUserRepo,
            tokenRepo,
            tokenGenerator,
            passwordEncoder
    );

    @Test
    void itShouldReturnTokenAlreadyUsedWhenTokenIsInBlocklistTokens() {
        var token = new Token("1234");
        when(tokenRepo.find("1234")).thenReturn(Option.of(token));

        Either<UseCaseError, Void> response = passwordResetter
                .updatePassword(token, PasswordResetInformation.of("newpa55word"));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(TokenError.TOKEN_ALREADY_USED);
    }

    @Test
    void itShouldReturnInvalidTokenWhenCannotParseAuthUserIdFromToken() {
        var token = new Token("1234");
        when(tokenRepo.find("1234")).thenReturn(Option.none());
        when(tokenGenerator.parseToken(token)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = passwordResetter
                .updatePassword(token, PasswordResetInformation.of("newpa55word"));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(TokenError.INVALID_TOKEN);
    }

    @Test
    void itShouldReturnUserNotFoundWhenThereIsNoUserWithIdProvided() {
        var token = new Token("1234");
        var authUserId = new AuthUserId();
        when(tokenRepo.find("1234")).thenReturn(Option.none());
        when(tokenGenerator.parseToken(token)).thenReturn(Option.of(authUserId));
        when(authUserRepo.findById(authUserId)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = passwordResetter
                .updatePassword(token, PasswordResetInformation.of("newpa55word"));

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void itShouldUpdateUserWithPasswordUpdatedEncoded() {
        var token = new Token("1234");
        var authUser = new AuthUserBuilder().build();
        when(tokenRepo.find("1234")).thenReturn(Option.none());
        when(tokenGenerator.parseToken(token)).thenReturn(Option.of(authUser.id()));
        when(authUserRepo.findById(authUser.id())).thenReturn(Option.of(authUser));
        when(authUserRepo.update(any())).thenReturn(Try.success(null));
        when(passwordEncoder.encode("newpa55word")).thenReturn("wordnewpa55");

        Either<UseCaseError, Void> response = passwordResetter
                .updatePassword(token, PasswordResetInformation.of("newpa55word"));

        verify(authUserRepo, times(1)).update(authUserArgumentCaptor.capture());
        verify(tokenRepo, times(1)).store(token);
        var authUserUpdated = authUserArgumentCaptor.getValue();
        assertThat(authUserUpdated.password()).isEqualTo("wordnewpa55");
    }
}
