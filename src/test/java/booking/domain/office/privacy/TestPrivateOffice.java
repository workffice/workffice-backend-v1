package booking.domain.office.privacy;

import booking.domain.booking.Booking;
import booking.factories.BookingBuilder;
import com.google.common.collect.ImmutableList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPrivateOffice {

    @Test
    void itShouldReturnFalseWhenProposedScheduleTimeHasConflictsWithAtLeastAnotherBooking() {
        var privacyPrivate = new PrivateOffice(1);
        var argZone = ZoneId.of("America/Argentina/Buenos_Aires");
        var booking14To15 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone
                )).build();
        var booking9To12 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        9, 0, 0, 0,
                        argZone
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        argZone
                )).build();
        List<Booking> existentBookings = ImmutableList.of(
                booking9To12,
                booking14To15
        );

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                2018, 12, 11,
                10, 0, 0
        );
        var proposedEndTime = LocalDateTime.of(
                2018, 12, 8,
                14, 0, 0
        );
        var response = privacyPrivate.canBeBooked(proposedStartTime, proposedEndTime, existentBookings);

        assertThat(response).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenProposedScheduleTimeHasNoConflictsWithAnyOtherBooking() {
        var privacyPrivate = new PrivateOffice(1);
        var argZone = ZoneId.of("America/Argentina/Buenos_Aires");
        var booking14To15 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        argZone
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        argZone
                )).build();
        var booking9To12 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        9, 0, 0, 0,
                        argZone
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        argZone
                )).build();
        List<Booking> existentBookings = ImmutableList.of(
                booking9To12,
                booking14To15
        );

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                2018, 12, 11,
                15, 0, 0
        );
        var proposedEndTime = LocalDateTime.of(
                2018, 12, 8,
                16, 0, 0
        );
        var response = privacyPrivate.canBeBooked(proposedStartTime, proposedEndTime, existentBookings);

        assertThat(response).isTrue();
    }
}
