package shared.infrastructure;

import shared.domain.email.EmailSender;
import shared.domain.email.Message;

import java.util.List;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SpringEmailSender implements EmailSender {
    
    private final JavaMailSender javaMailSender;
    
    public SpringEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }
    
    @Async
    @Override
    public void send(Message message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setText(message.body());
        mailMessage.setTo(message.recipient());
        mailMessage.setSubject(message.subject());
        mailMessage.setFrom("workffice.ar@gmail.com");
        javaMailSender.send(mailMessage);
    }
   public void sendBatch(List<Message> messages) {
        messages.forEach(this::send);
    }
}
