package booking.application.dto.booking;

import lombok.Value;

import java.time.LocalDateTime;

@Value(staticConstructor = "of")
public class BookingResponse {
    String             id;
    String             status;
    Integer            attendeesQuantity;
    Integer            totalAmount;
    LocalDateTime      created;
    LocalDateTime      startTime;
    LocalDateTime      endTime;
    PaymentInformation payment;
    String             officeId;
    String             officeName;
    String             officeBranchId;

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
