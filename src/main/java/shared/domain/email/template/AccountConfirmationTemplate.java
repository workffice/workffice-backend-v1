package shared.domain.email.template;

import lombok.EqualsAndHashCode;

import java.util.HashMap;

import static java.lang.String.format;

@EqualsAndHashCode
public class AccountConfirmationTemplate implements Template {

    public static final String TEMPLATE_NAME = "ACCOUNT_CONFIRMATION";
    private final String confirmationToken;
    private final String host;

    public AccountConfirmationTemplate(String confirmationToken, String host) {
        this.confirmationToken = confirmationToken;
        this.host = host;
    }

    public String templateName() {
        return TEMPLATE_NAME;
    }

    @Override
    public String subject() {
        return "Confirma tu cuenta";
    }

    @Override
    public String from() {
        return "workffice.ar@gmail.com";
    }

    @Override
    public HashMap<String, String> substitutionData() {
        return new HashMap<>() {{
            put("url", format(
                    "%s/auth/confirmation?token=%s",
                    host,
                    confirmationToken
            ));
        }};
    }

    @Override
    public String plainTextBody() {
        return format(
                "Confirma tu cuenta accediendo a este link %s/auth/confirmation?token=%s",
                host,
                confirmationToken
        );
    }
}
