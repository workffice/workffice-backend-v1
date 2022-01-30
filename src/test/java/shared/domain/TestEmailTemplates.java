package shared.domain;

import io.vavr.control.Option;
import shared.domain.email.template.AccountConfirmationTemplate;
import shared.domain.email.template.EmailTemplates;
import shared.domain.email.template.Template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestEmailTemplates {
    
    @Test
    void itShouldReturnEmptyWhenTemplateNameDoesNotExist() {
        EmailTemplates emailTemplates = new EmailTemplates();
        Template template = mock(Template.class);
        when(template.templateName()).thenReturn("UNEXISTENT_TEMPLATE");
        
        Option<String> id = emailTemplates.sendgridTemplateId(template);
        
        assertThat(id.isEmpty()).isTrue();
    }
    
    @Test
    void itShouldReturnSomeStringWhenTemplateExist() {
        EmailTemplates emailTemplates = new EmailTemplates();
        
        Option<String> id = emailTemplates
                .sendgridTemplateId(new AccountConfirmationTemplate("1", "localhost:3000"));

        assertThat(id.isDefined()).isTrue();
    }
}
