package shared.domain.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import shared.domain.email.template.Template;

@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Message {
    private final String recipient;
    private final Template template;
    
    public Template template() {
        return template;
    }
    
    public String from() {
        return template.from();
    }
    
    public String subject() {
        return template.subject();
    }
    
    public String recipient() {
        return recipient;
    }
    
    public String body() {
        return template.plainTextBody();
    }
}
