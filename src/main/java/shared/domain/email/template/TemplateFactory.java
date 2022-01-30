package shared.domain.email.template;

import shared.infrastructure.config.ConfigUtil;
import shared.infrastructure.config.EnvironmentConfigurationError;

import java.time.LocalDateTime;
import java.util.HashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class TemplateFactory {
    private final Environment env;

    public TemplateFactory(Environment env) throws EnvironmentConfigurationError {
        this.env = env;
        ConfigUtil.verifyEnvVariables(new HashMap<>() {{
            put("CLIENT_HOST", env.getProperty("CLIENT_HOST"));
        }});
    }

    public Template createAccountConfirmationTemplate(String confirmationToken) {
        String host = env.getProperty("CLIENT_HOST");
        return new AccountConfirmationTemplate(confirmationToken, host);
    }

    public Template createPasswordResetTemplate(String token) {
        String host = env.getProperty("CLIENT_HOST");
        return new PasswordResetTemplate(token, host);
    }

    public Template createCollaboratorInvitationTemplate(String officeBranchName, String token) {
        String host = env.getProperty("CLIENT_HOST");
        return new CollaboratorInvitationTemplate(host, officeBranchName, token);
    }

    public Template createBookingPaymentFailedTemplate() {
        return new BookingPaymentFailedTemplate();
    }

    public Template createBookingPaymentAcceptedTemplate(
            String        bookingId,
            String        officeName,
            LocalDateTime startScheduleTime,
            LocalDateTime endScheduleTime,
            Integer       bookingHoursQuantity,
            Float         totalPrice,
            String        province,
            String        city,
            String        zipCode,
            String        street
    ) {
        String host = env.getProperty("CLIENT_HOST");
        var bookingScheduleDate = startScheduleTime.toLocalDate().toString();
        var bookingScheduleTime = format(
                "Desde %s hasta %s",
                startScheduleTime.toLocalTime(),
                endScheduleTime.toLocalTime()
        );
        return new BookingPaymentAcceptedTemplate(
                host,
                bookingId,
                officeName,
                bookingScheduleDate,
                bookingScheduleTime,
                bookingHoursQuantity,
                totalPrice,
                province,
                city,
                zipCode,
                street
        );
    }
}
