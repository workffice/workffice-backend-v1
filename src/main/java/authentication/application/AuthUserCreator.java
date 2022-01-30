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
import io.vavr.control.Either;
import shared.application.UseCaseError;
import shared.domain.EventBus;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.TemplateFactory;

import org.springframework.stereotype.Service;

@Service
public class AuthUserCreator {

    private final EventBus           eventBus;
    private final EmailSender        emailSender;
    private final TokenGenerator     tokenGenerator;
    private final PasswordEncoder    passwordEncoder;
    private final TemplateFactory    templateFactory;
    private final AuthUserRepository authUserRepository;

    public AuthUserCreator(
            EventBus           eventBus,
            EmailSender        emailSender,
            TokenGenerator     tokenGenerator,
            TemplateFactory    templateFactory,
            PasswordEncoder    passwordEncoder,
            AuthUserRepository authUserRepository
    ) {
        this.eventBus           = eventBus;
        this.emailSender        = emailSender;
        this.tokenGenerator     = tokenGenerator;
        this.passwordEncoder    = passwordEncoder;
        this.templateFactory    = templateFactory;
        this.authUserRepository = authUserRepository;
    }

    private void sendConfirmationEmail(AuthUser authUser) {
        Token confirmationToken = tokenGenerator.create(authUser);
        emailSender.send(
                Message.builder()
                        .recipient(authUser.email())
                        .template(templateFactory.createAccountConfirmationTemplate(confirmationToken.token()))
                        .build()
        );
    }

    public Either<UseCaseError, Void> createUser(AuthUserId id, UserInformation userInformation) {
        AuthUser authUser = AuthUser.createNew(
                id,
                userInformation.getEmail(),
                passwordEncoder.encode(userInformation.getPassword())
        );
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent(
                id.toString(),
                userInformation.getEmail(),
                userInformation.getType()
        );

        return authUserRepository.store(authUser)
                .toEither((UseCaseError) UserError.USER_EMAIL_ALREADY_EXISTS)
                .peek(v -> {
                    sendConfirmationEmail(authUser);
                    eventBus.publish(userCreatedEvent);
                });
    }
}
