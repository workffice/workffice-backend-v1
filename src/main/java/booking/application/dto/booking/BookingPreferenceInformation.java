package booking.application.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
@Getter
public class BookingPreferenceInformation {
    private final String bookingId;
    private final String renterEmail;
    private final String officeName;
    private final Float  price;
    private final String description;
}
