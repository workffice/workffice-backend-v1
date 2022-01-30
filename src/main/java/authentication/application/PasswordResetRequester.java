package authentication.application;

import authentication.application.dto.user.UserError;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUserRepository;
import io.vavr.control.Either;
import shared.application.UseCaseError;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.TemplateFactory;

import org.springframework.stereotype.Service;

@Service
public class PasswordResetRequester {

    private final EmailSender        emailSender;
    private final TokenGenerator     tokenGenerator;
    private final TemplateFactory    templateFactory;
    private final AuthUserRepository authUserRepository;

    public PasswordResetRequester(
            EmailSender        emailSender,
            TokenGenerator     tokenGenerator,
            TemplateFactory    templateFactory,
            AuthUserRepository authUserRepository
    ) {
        this.emailSender = emailSender;
        this.tokenGenerator = tokenGenerator;
        this.templateFactory = templateFactory;
        this.authUserRepository = authUserRepository;
    }

    public Either<UseCaseError, Void> requestPasswordReset(String email) {
        return authUserRepository
                .findByEmail(email)
                .toEither((UseCaseError) UserError.USER_NOT_FOUND)
                .map(tokenGenerator::create)
                .map(token -> {
                    var message = Message.builder()
                            .recipient(email)
                            .template(templateFactory.createPasswordResetTemplate(token.token()))
                            .build();
                    emailSender.send(message);
                    return null;
                });
    }

}
