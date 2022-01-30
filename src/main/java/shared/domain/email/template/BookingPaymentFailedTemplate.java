package shared.domain.email.template;

import java.util.HashMap;

public class BookingPaymentFailedTemplate implements Template {
    public static final String TEMPLATE_NAME = "BOOKING_PAYMENT_FAILED";

    @Override
    public String templateName() {
        return TEMPLATE_NAME;
    }

    @Override
    public String subject() {
        return "Tu reserva no pudo completarse correctamente";
    }

    @Override
    public String from() {
        return "workffice.ar@gmail.com";
    }

    @Override
    public HashMap<String, String> substitutionData() {
        return new HashMap<>();
    }

    @Override
    public String plainTextBody() {
        return "Tu pago no fue procesado correctamente. Por favor vuelva a intentar antes de que la reserva expire";
    }
}
