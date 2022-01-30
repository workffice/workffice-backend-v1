package backoffice.application.collaborator;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.domain.collaborator.CollaboratorTokenGenerator;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.TemplateFactory;

import org.springframework.stereotype.Service;

@Service
public class InvitationEmailSender {
    private final EmailSender                emailSender;
    private final TemplateFactory            templateFactory;
    private final CollaboratorTokenGenerator tokenGenerator;

    public InvitationEmailSender(
            EmailSender                emailSender,
            TemplateFactory            templateFactory,
            CollaboratorTokenGenerator tokenGenerator
    ) {
        this.emailSender     = emailSender;
        this.tokenGenerator  = tokenGenerator;
        this.templateFactory = templateFactory;
    }

    public void sendInvitation(Collaborator collaborator) {
        CollaboratorToken confirmationToken = tokenGenerator.createToken(collaborator);
        var message = Message.builder()
                .recipient(collaborator.email())
                .template(templateFactory.createCollaboratorInvitationTemplate(
                        collaborator.officeBranch().name(),
                        confirmationToken.getToken()
                ))
                .build();
        emailSender.send(message);
    }
}
