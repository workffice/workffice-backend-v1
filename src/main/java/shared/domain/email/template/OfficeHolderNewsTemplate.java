package shared.domain.email.template;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

import static java.lang.String.format;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class OfficeHolderNewsTemplate implements Template {
    public static String TEMPLATE_NAME = "OFFICE_HOLDER_NEWS";

    private final String subject;
    private final String title;
    private final String body;

    @Override
    public String templateName() {
        return TEMPLATE_NAME;
    }

    @Override
    public String subject() {
        return subject;
    }

    @Override
    public String from() {
        return "workffice.ar@gmail.com";
    }

    @Override
    public HashMap<String, String> substitutionData() {
        return new HashMap<>() {{
            put("title", title);
            put("body", body);
            put("subject", subject);
        }};
    }

    @Override
    public String plainTextBody() {
        return format("%s \n %s", title, body);
    }
}
