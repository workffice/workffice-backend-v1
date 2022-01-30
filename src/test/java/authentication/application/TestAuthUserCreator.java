package authentication.application;

import authentication.application.dto.user.UserError;
import authentication.application.dto.user.UserInformation;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import authentication.domain.user.AuthUserRepository;
import authentication.domain.user.PasswordEncoder;
import authentication.domain.user.UserCreatedEvent;
import authentication.domain.user.UserEmailAlreadyExist;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;
import shared.domain.EventBus;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.AccountConfirmationTemplate;
import shared.domain.email.template.TemplateFactory;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestAuthUserCreator {

    EventBus eventBus = mock(EventBus.class);
    EmailSender emailSender = mock(EmailSender.class);
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    TemplateFactory templateFactory = mock(TemplateFactory.class);
    AuthUserRepository authUserRepository = mock(AuthUserRepository.class);
    ArgumentCaptor<AuthUser> userArgumentCaptor = ArgumentCaptor.forClass(AuthUser.class);

    AuthUserCreator creator = new AuthUserCreator(
            eventBus,
            emailSender,
            tokenGenerator,
            templateFactory,
            passwordEncoder,
            authUserRepository
    );

    @Test
    void itShouldStoreNewUserWithEmailAndIdProvided() {
        AuthUserId id = new AuthUserId();
        when(authUserRepository.store(any())).thenReturn(Try.success(null));
        when(tokenGenerator.create(any())).thenReturn(new Token("123"));

        creator.createUser(id, new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER"));

        verify(authUserRepository).store(userArgumentCaptor.capture());
        AuthUser authUser = userArgumentCaptor.getValue();
        assertThat(authUser.email()).isEqualTo("test@mail.com");
        assertThat(authUser.id()).isEqualTo(id);
    }

    @Test
    void itShouldStoreNewUserWithPendingStatus() {
        AuthUserId id = new AuthUserId();
        when(authUserRepository.store(any())).thenReturn(Try.success(null));
        when(tokenGenerator.create(any())).thenReturn(new Token("123"));

        creator.createUser(id, new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER"));

        verify(authUserRepository).store(userArgumentCaptor.capture());
        AuthUser authUser = userArgumentCaptor.getValue();
        assertThat(authUser.isActive()).isFalse();
    }

    @Test
    void itShouldStoreNewUserWithPasswordEncoded() {
        when(passwordEncoder.encode(anyString())).thenReturn("super_secret_password");
        when(authUserRepository.store(any())).thenReturn(Try.success(null));
        when(tokenGenerator.create(any())).thenReturn(new Token("123"));

        creator.createUser(new AuthUserId(), new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER"));
        verify(authUserRepository).store(userArgumentCaptor.capture());
        AuthUser authUser = userArgumentCaptor.getValue();
        assertThat(authUser.password()).isEqualTo("super_secret_password");
    }

    @Test
    void itShouldPublishUserCreatedEvent() {
        AuthUserId id = new AuthUserId();
        when(authUserRepository.store(any())).thenReturn(Try.success(null));
        when(tokenGenerator.create(any())).thenReturn(new Token("123"));

        creator.createUser(id, new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER"));

        verify(eventBus, times(1)).publish(
                new UserCreatedEvent(id.toString(), "test@mail.com", "OFFICE_HOLDER")
        );
    }

    @Test
    void itShouldReturnUserCreationErrorWhenStoreFails() {
        AuthUserId id = new AuthUserId();
        when(authUserRepository.store(any())).thenReturn(
                Try.failure(new UserEmailAlreadyExist())
        );

        Either<UseCaseError, Void> response = creator.createUser(
                id,
                new UserInformation("test@mail.com", "1234", "OFFICE_HOLDER")
        );

        assertThat(response.getLeft()).isEqualTo(UserError.USER_EMAIL_ALREADY_EXISTS);
    }

    @Test
    void itShouldSendEmailAfterUserIsCreated() {
        AuthUserId id = new AuthUserId();
        when(authUserRepository.store(any())).thenReturn(Try.success(null));
        when(tokenGenerator.create(any())).thenReturn(new Token("1234"));
        when(templateFactory.createAccountConfirmationTemplate("1234"))
                .thenReturn(new AccountConfirmationTemplate("1234", "localhost:3000"));

        creator.createUser(id, new UserInformation("test@mail.com", "12", "OFFICE_HOLDER"));

        verify(emailSender, times(1)).send(
                Message.builder()
                        .recipient("test@mail.com")
                        .template(new AccountConfirmationTemplate("1234", "localhost:3000"))
                        .build()
        );
    }
}
