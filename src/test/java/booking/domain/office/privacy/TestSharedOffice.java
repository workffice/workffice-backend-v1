package booking.domain.office.privacy;

import booking.domain.booking.Booking;
import booking.factories.BookingBuilder;
import com.google.common.collect.ImmutableList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSharedOffice {

    @Test
    void itShouldReturnTrueWhenConflictBookingsAreLessThanTablesQuantity() {
        var argZone = ZoneId.of("America/Argentina/Buenos_Aires");
        var booking14To15_1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone)).build();
        var booking14To15_2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone)).build();
        List<Booking> existentBookings = ImmutableList.of(
                booking14To15_2,
                booking14To15_1
        );
        SharedOffice privacyShared = new SharedOffice(3, 10);

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                2018, 12, 8,
                17, 0, 0
        );
        var proposedEndTime = LocalDateTime.of(
                2018, 12, 8,
                18, 0, 0
        );
        var response = privacyShared.canBeBooked(proposedStartTime, proposedEndTime, existentBookings);

        assertThat(response).isTrue();
    }

    @Test
    @DisplayName("conflict bookings are greater than tablesQuantity but " +
            "existent bookings does not have conflicts between each one")
    void itShouldReturnTrue() {
        var argZone = ZoneId.of("America/Argentina/Buenos_Aires");
        var booking14To15_1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone)).build();
        var booking14To15_2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone)).build();
        var booking16To17 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        16, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        17, 0, 0, 0,
                        argZone)).build();
        List<Booking> existentBookings = ImmutableList.of(
                booking14To15_2,
                booking14To15_1,
                booking16To17
        );
        SharedOffice privacyShared = new SharedOffice(3, 10);

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                2018, 12, 8,
                16, 0, 0
        );
        var proposedEndTime = LocalDateTime.of(
                2018, 12, 8,
                20, 0, 0
        );
        var response = privacyShared.canBeBooked(proposedStartTime, proposedEndTime, existentBookings);

        assertThat(response).isTrue();
    }

    @Test
    @DisplayName("conflict bookings are greater than tablesQuantity and " +
            "there is at least one existent booking with conflicts with" +
            "other existent bookings greater than tablesQuantity")
    void itShouldReturnFalse() {
        var argZone = ZoneId.of("America/Argentina/Buenos_Aires");
        var booking16To17_1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        16, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        17, 0, 0, 0,
                        argZone)).build();
        var booking16To17_2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        16, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        17, 0, 0, 0,
                        argZone)).build();
        var booking13To17 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        13, 0, 0, 0,
                        argZone))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        17, 0, 0, 0,
                        argZone)).build();
        List<Booking> existentBookings = ImmutableList.of(
                booking16To17_1,
                booking16To17_2,
                booking13To17
        );
        SharedOffice privacyShared = new SharedOffice(3, 10);

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                2018, 12, 8,
                17, 0, 0
        );
        var proposedEndTime = LocalDateTime.of(
                2018, 12, 8,
                20, 0, 0
        );
        var response = privacyShared.canBeBooked(proposedStartTime, proposedEndTime, existentBookings);

        assertThat(response).isFalse();
    }
}
