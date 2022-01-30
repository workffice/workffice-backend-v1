package booking.domain.booking;

import booking.factories.OfficeBuilder;
import io.vavr.control.Try;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBooking {
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void itShouldReturnFailureWhenTryingToCreateABookingWithInvalidDate() {
        // End time is equal to start time
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        Try<Booking> booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        );

        assertThat(booking.isFailure()).isTrue();

        // End time is before start time
        var startTime2 = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime2 = ZonedDateTime.of(
                2018, 12, 9, 13, 0, 0, 0,
                timezoneARG
        );
        Try<Booking> booking2 = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime2,
                endTime2,
                "test@email.com",
                10
        );

        assertThat(booking2.isFailure()).isTrue();
    }

    @Test
    void itShouldReturnFailureWhenTryingToCreateABookingWithMinutesDifferentFrom0() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 15, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 15, 0, 0, 0,
                timezoneARG
        );
        Try<Booking> booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        );

        assertThat(booking.isFailure()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"18:00", "18:30", "17:20", "18:01"}) // In UTC
    void itShouldReturnTrueWhenBookingHasConflictsWithProposedStartHour(String invalidStartHour) {
        var startTime = ZonedDateTime.of(
                2018, 12, 9,
                14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9,
                16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.parse(invalidStartHour)
        );
        var proposedEndTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(21, 0)
        );
        var response = booking.hasConflictsWithProposedTime(proposedStartTime, proposedEndTime);

        assertThat(response).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"19:00", "18:30", "17:20", "18:01"})
    void itShouldReturnTrueWhenBookingHasConflictsWithProposedEndHour(String invalidEndHour) {
        var startTime = ZonedDateTime.of(
                2018, 12, 9,
                14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9,
                16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(15, 0)
        );
        var proposedEndTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.parse(invalidEndHour)
        );
        var response = booking.hasConflictsWithProposedTime(
                proposedStartTime,
                proposedEndTime
        );

        assertThat(response).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenProposedTimeContainsScheduleTime() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9,
                14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9,
                16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        // UTC is 3 hours greater than ARG timezone
        var proposedStartTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(17, 0)
        );
        var proposedEndTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(21, 0)
        );
        var response = booking.hasConflictsWithProposedTime(proposedStartTime, proposedEndTime);

        assertThat(response).isTrue();
    }

    @Test
    void itShouldReturnFalseWhenBookingHasNoConflictsWithProposedTime() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9,
                14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9,
                16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        var proposedStartTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(12, 0)
        );
        var proposedEndTime = LocalDateTime.of(
                LocalDate.of(2018, 12, 9),
                LocalTime.of(13, 0)
        );
        var response = booking.hasConflictsWithProposedTime(
                proposedStartTime,
                proposedEndTime
        );

        assertThat(response).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenProposedHourIsTheSameButTheProposedDateIsDifferent() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9,
                14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9,
                16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        var response = booking.hasConflictsWithProposedTime(
                LocalDateTime.of(2018, 12, 10, 17, 0, 0),
                LocalDateTime.of(2018, 12, 10, 18, 0, 0)
        );

        assertThat(response).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenBookingIsInScheduleStatus() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 16, 0, 0, 0,
                timezoneARG
        );
        // Booking is created as pending by default
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();
        booking.markAsScheduled(new PaymentInformation());

        assertThat(booking.isActive()).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenBookingIsInPendingStatusAndItHasNotPassedOneHour() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 16, 0, 0, 0,
                timezoneARG
        );
        // Booking is created as pending by default
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();
        assertThat(booking.isActive()).isTrue();
    }

    @Test
    void itShouldReturnScheduleTimeInBookingTimezone() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();

        assertThat(booking.startScheduleTime()).isEqualTo(LocalDateTime.of(
                2018, 12, 9,
                14, 0, 0
        ));
        assertThat(booking.endScheduleTime()).isEqualTo(LocalDateTime.of(
                2018, 12, 9,
                16, 0, 0
        ));
    }

    @Test
    void itShouldReturnCancelledWhenBookingIsPendingAndHasPassedOneHour() {
        var startTime = ZonedDateTime.of(
                2018, 12, 9, 14, 0, 0, 0,
                timezoneARG
        );
        var endTime = ZonedDateTime.of(
                2018, 12, 9, 16, 0, 0, 0,
                timezoneARG
        );
        var booking = Booking.create(
                new BookingId(),
                new OfficeBuilder().build(),
                startTime,
                endTime,
                "test@email.com",
                10
        ).get();
        var oneHourMore = LocalDateTime.now(Clock.systemUTC()).plusHours(1);

        try (MockedStatic<LocalDateTime> mockLocalDate = Mockito.mockStatic(LocalDateTime.class)) {
            mockLocalDate.when(() -> LocalDateTime.now(Clock.systemUTC())).thenReturn(oneHourMore);

            var response = booking.toResponse();

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
        }
    }
}
