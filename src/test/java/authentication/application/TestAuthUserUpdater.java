package authentication.application;

import authentication.application.dto.user.UserError;
import authentication.application.dto.user.UserUpdateInformation;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
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

public class TestAuthUserUpdater {
    AuthUserRepository authUserRepo = mock(AuthUserRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    ArgumentCaptor<AuthUser> authUserArgumentCaptor = ArgumentCaptor.forClass(AuthUser.class);

    UserUpdateInformation info = UserUpdateInformation.of(
            "Juancito",
            "Perez",
            "G Mistral 5885",
            "Una persona asombrosa",
            "image.url"
    );
    AuthUserUpdater updater = new AuthUserUpdater(authUserRepo, authUserValidator);

    @Test
    void itShouldReturnAuthUserNotFoundWhenThereIsNoUserWithIdSpecified() {
        var authUserId = new AuthUserId();
        when(authUserRepo.findById(authUserId)).thenReturn(Option.none());

        Either<UseCaseError, Void> response = updater.update(authUserId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthenticatedUserIsNotTheSameAsTheOneModified() {
        var authUser = new AuthUserBuilder().build();
        when(authUserRepo.findById(authUser.id())).thenReturn(Option.of(authUser));
        when(authUserValidator.isSameUserAsAuthenticated(authUser.email())).thenReturn(false);

        Either<UseCaseError, Void> response = updater.update(authUser.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(UserError.FORBIDDEN);
    }

    @Test
    void itShouldUpdateAuthUserWithInfoSpecified() {
        var authUser = new AuthUserBuilder().build();
        when(authUserRepo.findById(authUser.id())).thenReturn(Option.of(authUser));
        when(authUserValidator.isSameUserAsAuthenticated(authUser.email())).thenReturn(true);
        when(authUserRepo.update(any())).thenReturn(Try.success(null));

        Either<UseCaseError, Void> response = updater.update(authUser.id(), info);

        assertThat(response.isRight()).isTrue();
        verify(authUserRepo, times(1)).update(authUserArgumentCaptor.capture());
        var authUserUpdated = authUserArgumentCaptor.getValue();
        assertThat(authUserUpdated.toResponse().getName()).isEqualTo(info.getName());
        assertThat(authUserUpdated.toResponse().getProfileImage()).isEqualTo(info.getProfileImage());
        assertThat(authUserUpdated.toResponse().getLastname()).isEqualTo(info.getLastname());
        assertThat(authUserUpdated.toResponse().getAddress()).isEqualTo(info.getAddress());
        assertThat(authUserUpdated.toResponse().getAddress()).isEqualTo(info.getAddress());
    }
}
