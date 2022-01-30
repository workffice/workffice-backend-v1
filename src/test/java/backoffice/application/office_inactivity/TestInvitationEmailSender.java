package backoffice.application.office_inactivity;

import backoffice.application.collaborator.InvitationEmailSender;
import backoffice.domain.collaborator.CollaboratorToken;
import backoffice.domain.collaborator.CollaboratorTokenGenerator;
import backoffice.factories.CollaboratorBuilder;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.CollaboratorInvitationTemplate;
import shared.domain.email.template.TemplateFactory;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestInvitationEmailSender {
    EmailSender emailSender = mock(EmailSender.class);
    CollaboratorTokenGenerator tokenGenerator = mock(CollaboratorTokenGenerator.class);
    TemplateFactory templateFactory = mock(TemplateFactory.class);
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    InvitationEmailSender invitationEmailSender = new InvitationEmailSender(
            emailSender,
            templateFactory,
            tokenGenerator
    );

    @Test
    void itShouldCreateTokenUsingCollaborator() {
        var collaborator = new CollaboratorBuilder().build();
        when(tokenGenerator.createToken(any())).thenReturn(CollaboratorToken.of("12344"));

        invitationEmailSender.sendInvitation(collaborator);

        verify(tokenGenerator, times(1)).createToken(collaborator);
    }

    @Test
    void itShouldSendEmailToCollaboratorEmailAndUsingCollaboratorInvitationTemplate() {
        var collaborator = new CollaboratorBuilder().build();
        when(tokenGenerator.createToken(any())).thenReturn(CollaboratorToken.of("12344"));
        var collaboratorInvitationTemplate = new CollaboratorInvitationTemplate(
                "localhost:3000",
                collaborator.officeBranch().name(),
                "12344"
        );
        when(templateFactory.createCollaboratorInvitationTemplate(collaborator.officeBranch().name(), "12344"))
                .thenReturn(collaboratorInvitationTemplate);

        invitationEmailSender.sendInvitation(collaborator);

        verify(emailSender, times(1)).send(messageArgumentCaptor.capture());
        Message message = messageArgumentCaptor.getValue();
        assertThat(message.template()).isEqualTo(new CollaboratorInvitationTemplate(
                "localhost:3000",
                collaborator.officeBranch().name(),
                "12344"
        ));

    }
}
