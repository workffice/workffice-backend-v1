package booking.application.dto.booking;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
public class BookingInformation {
    private Integer attendeesQuantity;
    // Start time and end time are considered in ARG timezone
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String membershipAcquisitionId;

    public static BookingInformation of(
            Integer attendeesQuantity,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return new BookingInformation(
                attendeesQuantity,
                startTime,
                endTime,
                null
        );
    }

    public static BookingInformation of(
            Integer attendeesQuantity,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String membershipAcquisitionId
    ) {
        return new BookingInformation(
                attendeesQuantity,
                startTime,
                endTime,
                membershipAcquisitionId
        );
    }
}
