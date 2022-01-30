package authentication.application;

import authentication.application.dto.user.UserError;
import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUserRepository;
import authentication.factories.AuthUserBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import shared.application.UseCaseError;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.PasswordResetTemplate;
import shared.domain.email.template.TemplateFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestPasswordResetRequester {
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    AuthUserRepository authUserRepo = mock(AuthUserRepository.class);
    EmailSender emailSender = mock(EmailSender.class);
    TemplateFactory templateFactory = mock(TemplateFactory.class);

    PasswordResetRequester passwordResetRequester = new PasswordResetRequester(
            emailSender,
            tokenGenerator,
            templateFactory,
            authUserRepo
    );

    @Test
    void itShouldReturnUserNotFoundWhenThereIsNoUserWithEmailProvided() {
        when(authUserRepo.findByEmail("john@wick.com")).thenReturn(Option.none());

        Either<UseCaseError, Void> response = passwordResetRequester
                .requestPasswordReset("john@wick.com");

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void itShouldCallEmailSenderWithPasswordResetTemplate() {
        var authUser = new AuthUserBuilder().build();
        when(authUserRepo.findByEmail(authUser.email())).thenReturn(Option.of(authUser));
        when(tokenGenerator.create(any())).thenReturn(new Token("1234"));
        when(templateFactory.createPasswordResetTemplate("1234"))
                .thenReturn(new PasswordResetTemplate("1234", "localhost:3000"));

        Either<UseCaseError, Void> response = passwordResetRequester
                .requestPasswordReset(authUser.email());

        assertThat(response.isRight()).isTrue();
        verify(emailSender, times(1)).send(
                new Message(authUser.email(), new PasswordResetTemplate("1234", "localhost:3000"))
        );
    }
}
