package report.domain;

import lombok.Value;

@Value(staticConstructor = "of")
public class TransactionAmountProjection {
    Integer year;
    String  month;
    Float   totalAmount;
}
