package report.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "bookings")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class Booking {
    @MongoId
    private final String    id;
    private final String    officeBranchId;
    private final String    officeId;
    private final Float     amount;
    private final LocalDate paymentDate;
    private final String    month;
    private final Integer   year;

    public static Booking create(
            String    id,
            String    officeBranchId,
            String    officeId,
            Float     amount,
            LocalDate paymentDate
    ) {
        return new Booking(
                id,
                officeBranchId,
                officeId,
                amount,
                paymentDate,
                paymentDate.getMonth().name(),
                paymentDate.getYear()
        );
    }
}
