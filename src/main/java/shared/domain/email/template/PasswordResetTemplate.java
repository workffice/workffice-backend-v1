package shared.domain.email.template;

import lombok.EqualsAndHashCode;

import java.util.HashMap;

import static java.lang.String.format;

@EqualsAndHashCode
public class PasswordResetTemplate implements Template {
    public static String TEMPLATE_NAME = "PASSWORD_RESET";
    private final String token;
    private final String host;

    public PasswordResetTemplate(String token, String host) {
        this.token = token;
        this.host = host;
    }

    @Override
    public String templateName() {
        return TEMPLATE_NAME;
    }

    @Override
    public String subject() {
        return "Reset your password";
    }

    @Override
    public String from() {
        return "workffice.ar@gmail.com";
    }

    @Override
    public HashMap<String, String> substitutionData() {
        return new HashMap<>() {{
            put("url", format(
                    "%s/auth/reset-password?token=%s",
                    host,
                    token
            ));
        }};
    }

    @Override
    public String plainTextBody() {
        return format(
                "Reset your password here %s/auth/reset-password?token=%s",
                host,
                token
        );
    }
}
