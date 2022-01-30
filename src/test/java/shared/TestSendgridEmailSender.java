package shared;

import com.google.inject.internal.util.ImmutableList;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.vavr.control.Option;
import shared.domain.email.Message;
import shared.domain.email.template.AccountConfirmationTemplate;
import shared.domain.email.template.EmailTemplates;
import shared.domain.email.template.Template;
import shared.infrastructure.SendgridEmailSender;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestSendgridEmailSender {
    
    SendGrid mockSendgridClient = mock(SendGrid.class);
    static Response sendGridResponseOk = new Response();
    static Response sendGridResponseError = new Response();
    ArgumentCaptor<Request> sendGridRequestCaptor = ArgumentCaptor.forClass(Request.class);
    Template template = new AccountConfirmationTemplate("123", "localhost:3000");
    Message message = Message.builder()
            .recipient("test@mail.com")
            .template(template)
            .build();
    
    @BeforeAll
    static void setUp() {
        sendGridResponseOk.setStatusCode(HttpStatus.OK.value());
        sendGridResponseError.setStatusCode(HttpStatus.BAD_REQUEST.value());
    }
    
    @Test
    void itShouldCallSendgridClientWithMailBuild() throws IOException {
        EmailTemplates mockEmailTemplates = mock(EmailTemplates.class);
        Personalization personalization = new Personalization();
        personalization.addSubstitution("test", "HELLO");
        personalization.addTo(new Email(message.recipient()));
        SendgridEmailSender emailSender = new SendgridEmailSender(mockEmailTemplates, mockSendgridClient);
        when(mockEmailTemplates.sendgridTemplateId(any(Template.class))).thenReturn(Option.of("a1"));
        when(mockEmailTemplates.sengridSubstitutionData(any(Template.class))).thenReturn(personalization);
        Mail expectedMail = new Mail();
        expectedMail.setSubject(message.subject());
        expectedMail.setFrom(new Email(message.from()));
        expectedMail.addContent(new Content("text/html", "_"));
        expectedMail.setTemplateId("a1");
        expectedMail.addPersonalization(personalization);
        when(mockSendgridClient.api(any())).thenReturn(sendGridResponseOk);
        
        emailSender.send(message);
        
        verify(mockSendgridClient).api(sendGridRequestCaptor.capture());
        Request requestCaptured = sendGridRequestCaptor.getValue();
        assertThat(requestCaptured.getBody()).isEqualTo(expectedMail.build());
    }
    
    @Test
    void itShouldRaiseExceptionWhenTemplateDoesNotExist() {
        Personalization personalization = new Personalization();
        EmailTemplates mockEmailTemplates = mock(EmailTemplates.class);
        SendgridEmailSender emailSender = new SendgridEmailSender(mockEmailTemplates, mockSendgridClient);
        when(mockEmailTemplates.sendgridTemplateId(any(Template.class))).thenReturn(Option.none());
        when(mockEmailTemplates.sengridSubstitutionData(any(Template.class))).thenReturn(personalization);
        
        assertThatThrownBy(() -> emailSender.send(message))
                .hasMessage("Template specified does not exist");
    }
    
    @Test
    void itShouldRetryOneTimeWhenAPIReturnError() throws IOException {
        Personalization personalization = new Personalization();
        EmailTemplates mockEmailTemplates = mock(EmailTemplates.class);
        SendgridEmailSender emailSender = new SendgridEmailSender(mockEmailTemplates, mockSendgridClient);
        when(mockEmailTemplates.sendgridTemplateId(any(Template.class))).thenReturn(Option.of("a1"));
        when(mockEmailTemplates.sengridSubstitutionData(any(Template.class))).thenReturn(personalization);
        when(mockSendgridClient.api(any())).thenReturn(sendGridResponseError);
    
        emailSender.send(message);

        verify(mockSendgridClient, times(2)).api(any());
    }
    
    @Test
    void itShouldRetryOneTimeWhenAPIThrowsException() throws IOException {
        Personalization personalization = new Personalization();
        EmailTemplates mockEmailTemplates = mock(EmailTemplates.class);
        SendgridEmailSender emailSender = new SendgridEmailSender(mockEmailTemplates, mockSendgridClient);
        when(mockEmailTemplates.sendgridTemplateId(any(Template.class))).thenReturn(Option.of("a1"));
        when(mockEmailTemplates.sengridSubstitutionData(any(Template.class))).thenReturn(personalization);
        when(mockSendgridClient.api(any())).thenThrow(IOException.class);
        
        emailSender.send(message);

        verify(mockSendgridClient, times(2)).api(any());
    }

    @Test
    void itShouldSendEmailToEachRecipient() throws IOException {
        Personalization personalization = new Personalization();
        EmailTemplates mockEmailTemplates = mock(EmailTemplates.class);
        SendgridEmailSender emailSender = new SendgridEmailSender(mockEmailTemplates, mockSendgridClient);
        when(mockEmailTemplates.sendgridTemplateId(any(Template.class))).thenReturn(Option.of("a1"));
        when(mockEmailTemplates.sengridSubstitutionData(any(Template.class))).thenReturn(personalization);
        when(mockSendgridClient.api(any())).thenReturn(sendGridResponseOk);
        var message1 = Message.builder()
                .recipient("test+1@mail.com")
                .template(template)
                .build();
        var message2 = Message.builder()
                .recipient("test+2@mail.com")
                .template(template)
                .build();

        emailSender.sendBatch(ImmutableList.of(message, message1, message2));

        verify(mockSendgridClient, times(3)).api(any());
    }
}
