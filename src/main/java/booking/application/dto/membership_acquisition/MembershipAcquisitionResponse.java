package booking.application.dto.membership_acquisition;

import lombok.Value;

import java.util.Set;

@Value(staticConstructor = "of")
public class MembershipAcquisitionResponse {
    String             id;
    String             membershipId;
    String             month;
    String             buyerEmail;
    Integer            price;
    String             status;
    Set<String>        accessDays;
    PaymentInformation paymentInformation;

    @Value(staticConstructor = "of")
    public static class PaymentInformation {
        Long   id;
        String externalId;
        Float  transactionAmount;
        Float  providerFee;
        String currency;
        String paymentMethodId;
        String paymentTypeId;
    }
}
