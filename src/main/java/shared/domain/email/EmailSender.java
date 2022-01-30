package shared.domain.email;

import java.util.List;

public interface EmailSender {
    
    void send(Message message);

    void sendBatch(List<Message> messages);
}
