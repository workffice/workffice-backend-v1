package shared.domain.email.template;

import com.sendgrid.helpers.mail.objects.Personalization;
import io.vavr.control.Option;

import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplates {

    public Option<String> sendgridTemplateId(Template template) {
        HashMap<String, String> templateIds = new HashMap<>() {{
            put(AccountConfirmationTemplate.TEMPLATE_NAME, "d-a459049cdf434eea8769d195cd3537d6");
            put(CollaboratorInvitationTemplate.TEMPLATE_NAME, "d-90f49ed6c35f413098988bf52289967e");
            put(PasswordResetTemplate.TEMPLATE_NAME, "d-69b17e2a6cbe45cdb535f7baf88248fd");
            put(BookingPaymentFailedTemplate.TEMPLATE_NAME, "d-68e16810eed94415a3e9e953332f58f7");
            put(BookingPaymentAcceptedTemplate.TEMPLATE_NAME, "d-a47ae326016b42168e999399676476ee");
            put(OfficeHolderNewsTemplate.TEMPLATE_NAME, "d-c7eb668fc941418189281dbfdd00addd");
        }};
        return Option.of(templateIds.get(template.templateName()));
    }

    public Personalization sengridSubstitutionData(Template template) {
        Personalization personalization = new Personalization();
        template.substitutionData()
                .forEach(personalization::addDynamicTemplateData);
        return personalization;
    }
}
