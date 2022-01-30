package booking.domain.payment_preference;

import lombok.Value;

@Value(staticConstructor = "of")
public class PaymentPreference {
    String id;
}
