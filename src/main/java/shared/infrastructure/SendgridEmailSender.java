package shared.infrastructure;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.vavr.control.Try;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.EmailTemplates;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SendgridEmailSender implements EmailSender {
    private final EmailTemplates emailTemplates;
    private final SendGrid sendGridClient;
    
    public SendgridEmailSender(EmailTemplates emailTemplates, SendGrid sendGridClient) {
        this.emailTemplates = emailTemplates;
        this.sendGridClient = sendGridClient;
    }
    
    private Mail constructMail(Message message) {
        Mail mail = new Mail();
        mail.setFrom(new Email(message.from()));
        mail.setSubject(message.subject());
        mail.addContent(new Content("text/html", "_"));
        Personalization personalization = emailTemplates.sengridSubstitutionData(message.template());
        personalization.addTo(new Email(message.recipient()));
        mail.addPersonalization(personalization);
        mail.setTemplateId(emailTemplates
                .sendgridTemplateId(message.template())
                .getOrElseThrow(() -> new RuntimeException("Template specified does not exist"))
        );
        return mail;
    }
    
    private Try<Response> callApi(Mail mail) {
        return Try.of(() -> {
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            return sendGridClient.api(request);
        });
    }
    
    private void logError(String message) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.error(message);
    }

    @Async
    @Override
    public void send(Message message) {
        Predicate<Response> isSuccessfulResponse = response ->
                response.getStatusCode() >= HttpStatus.OK.value()
                        && response.getStatusCode() < HttpStatus.MULTIPLE_CHOICES.value();
        Mail mail = constructMail(message);
        callApi(mail)
                .andThen(response -> LoggerFactory.getLogger(this.getClass()).info(response.getBody()))
                .filter(isSuccessfulResponse)
                .recoverWith(e -> this.callApi(mail))
                .filter(isSuccessfulResponse)
                .onFailure(NoSuchElementException.class, e -> logError("Sendgrid response came with error"))
                .onFailure(IOException.class, e -> logError("Network error"));
    }

    @Async
    @Override
    public void sendBatch(List<Message> messages) {
        messages.forEach(this::send);
    }
}
