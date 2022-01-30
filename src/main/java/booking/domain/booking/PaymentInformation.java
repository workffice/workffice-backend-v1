package booking.domain.booking;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "payment_informations")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "id")
@Getter
public class PaymentInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String externalId;
    @Column
    private Float transactionAmount;
    @Column
    private Float providerFee;
    @Column
    private String currency;
    @Column
    private String paymentMethodId;
    @Column
    private String paymentTypeId;

    public PaymentInformation(
            String externalId,
            Float  transactionAmount,
            Float  providerFee,
            String currency,
            String paymentMethodId,
            String paymentTypeId
    ) {
        this.externalId        = externalId;
        this.transactionAmount = transactionAmount;
        this.providerFee       = providerFee;
        this.currency          = currency;
        this.paymentMethodId   = paymentMethodId;
        this.paymentTypeId     = paymentTypeId;
    }
}
