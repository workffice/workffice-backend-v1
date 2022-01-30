package report.domain;

import lombok.Value;

@Value(staticConstructor = "of")
public class OfficeTransactionAmountProjection {
    String officeId;
    String month;
    Float  totalAmount;
}
