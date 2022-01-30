package shared;

import com.google.inject.internal.util.ImmutableList;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import shared.domain.email.Message;
import shared.domain.email.template.AccountConfirmationTemplate;
import shared.domain.email.template.Template;
import shared.infrastructure.SpringEmailSender;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {SMTPEmailConfig.class})
public class TestSpringEmailSender {
    
    static GreenMail greenMail = new GreenMail();
    @Autowired
    JavaMailSender javaMailSender;
    
    @BeforeAll
    public static void setUp() {
        greenMail.withConfiguration(GreenMailConfiguration
                .aConfig()
                .withUser("test", "test"));
        greenMail.start();
    }

    @BeforeEach
    public void setUpEach() {
        greenMail.reset();
    }
    
    @AfterAll
    public static void tearDown() {
        greenMail.stop();
    }
    
    @Test
    void itShouldSendEmailWithSpecifiedBody() throws MessagingException {
        SpringEmailSender sender = new SpringEmailSender(javaMailSender);
        Template template = new AccountConfirmationTemplate("123", "localhost:3000");
        sender.send(Message.builder()
                .recipient("john@doe.com")
                .template(template)
                .build());
        
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(GreenMailUtil.getBody(messages[0])).isEqualTo(template.plainTextBody());
        assertThat(messages[0].getSubject()).isEqualTo(template.subject());
        assertThat(messages[0].getAllRecipients().length).isEqualTo(1);
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("john@doe.com");
    }

    @Test
    void itShouldSendEmailToAllRecipientsWithSpecifiedBody() throws MessagingException {
        SpringEmailSender sender = new SpringEmailSender(javaMailSender);
        Template template = new AccountConfirmationTemplate("123", "localhost:3000");
        var message1 = Message.builder()
                .recipient("john@doe.com")
                .template(template)
                .build();
        var message2 = Message.builder()
                .recipient("john@wick.com")
                .template(template)
                .build();
        var message3 = Message.builder()
                .recipient("napoleon@gallardo.com")
                .template(template)
                .build();
        sender.sendBatch(ImmutableList.of(message1, message2, message3));

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages.length).isEqualTo(3);
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("john@doe.com");
        assertThat(messages[1].getAllRecipients()[0].toString()).isEqualTo("john@wick.com");
        assertThat(messages[2].getAllRecipients()[0].toString()).isEqualTo("napoleon@gallardo.com");
    }
}
