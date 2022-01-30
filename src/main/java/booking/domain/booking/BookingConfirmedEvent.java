package booking.domain.booking;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import shared.domain.DomainEvent;

import java.time.LocalDate;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode(callSuper = false)
public class BookingConfirmedEvent extends DomainEvent {
    private final String    bookingId;
    private final String    officeBranchId;
    private final String    officeId;
    private final Float     transactionAmount;
    private final LocalDate paymentDate;
    private final String    renterEmail;

    @Override
    public String getEventName() {
        return "BOOKING_CONFIRMED_EVENT";
    }
}
