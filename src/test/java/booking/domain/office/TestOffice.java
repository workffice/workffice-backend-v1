package booking.domain.office;

import booking.application.dto.booking.BookingError;
import booking.domain.booking.Booking;
import booking.domain.inactivity.InactivityId;
import booking.domain.inactivity.RecurringDay;
import booking.domain.inactivity.SpecificDate;
import booking.domain.office.privacy.PrivateOffice;
import booking.domain.office.privacy.SharedOffice;
import booking.factories.BookingBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOffice {

    ZoneId timeZoneARG = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void itShouldReturnFailureWhenProposedTimeForBookingIsInvalid() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(1, 2));

        // End time is one month before start time
        Either<UseCaseError, Booking> bookingOrError = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2018, 11, 8,
                        13, 0, 0, 0,
                        timeZoneARG
                ),
                new ArrayList<>()
        );

        assertThat(bookingOrError.isLeft()).isTrue();
        assertThat(bookingOrError.getLeft()).isEqualTo(BookingError.INVALID_SCHEDULE_TIME);
    }

    @Test
    void itShouldReturnFailureWhenOfficeIsUnavailableInSpecificDate() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(1, 2));
        var specificDateInactivity = new SpecificDate(new InactivityId(), LocalDate.of(2018, 12, 8));
        office.addInactivity(specificDateInactivity);

        Either<UseCaseError, Booking> bookingOrError = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2018, 12, 8,
                        13, 0, 0, 0,
                        timeZoneARG
                ),
                new ArrayList<>()
        );

        assertThat(bookingOrError.isLeft()).isTrue();
        assertThat(bookingOrError.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }

    @Test
    void itShouldReturnFailureWhenOfficeIsUnavailableInRecurringDay() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(1, 2));
        var recurringDayInactivity = new RecurringDay(new InactivityId(), DayOfWeek.WEDNESDAY);
        office.addInactivity(recurringDayInactivity);

        // 15th september of 2021 was wednesday
        Either<UseCaseError, Booking> bookingOrError2 = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2021, 9, 15,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2021, 9, 15,
                        13, 0, 0, 0,
                        timeZoneARG
                ),
                new ArrayList<>()
        );

        assertThat(bookingOrError2.isLeft()).isTrue();
        assertThat(bookingOrError2.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }

    @Test
    void itShouldReturnBookingWhenOfficeIsAvailableInSpecificDateAndOfficeHasNoPreviousBookings() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(1, 2));
        var specificDateInactivity = new SpecificDate(new InactivityId(), LocalDate.of(2018, 12, 8));
        office.addInactivity(specificDateInactivity);

        Either<UseCaseError, Booking> bookingOrError = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2018, 12, 9,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2018, 12, 9,
                        13, 0, 0, 0,
                        timeZoneARG
                ),
                new ArrayList<>()
        );

        assertThat(bookingOrError.isRight()).isTrue();
        assertThat(bookingOrError.get().toResponse().getStartTime()).isEqualTo(
                LocalDateTime.of(2018, 12, 9, 12, 0, 0)
        );
        assertThat(bookingOrError.get().toResponse().getEndTime()).isEqualTo(
                LocalDateTime.of(2018, 12, 9, 13, 0, 0)
        );
    }

    @Test
    void itShouldReturnBookingWhenOfficeIsAvailableInRecurringDateAndOfficeHasNoPreviousBookings() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(1, 2));
        var recurringDayInactivity = new RecurringDay(new InactivityId(), DayOfWeek.WEDNESDAY);
        office.addInactivity(recurringDayInactivity);

        // 16th september of 2021 was thursday
        Either<UseCaseError, Booking> bookingOrError2 = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2021, 9, 16,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2021, 9, 16,
                        13, 0, 0, 0,
                        timeZoneARG
                ),
                new ArrayList<>()
        );

        assertThat(bookingOrError2.isRight()).isTrue();
        assertThat(bookingOrError2.get().toResponse().getStartTime()).isEqualTo(
                LocalDateTime.of(2021, 9, 16, 12, 0, 0)
        );
        assertThat(bookingOrError2.get().toResponse().getEndTime()).isEqualTo(
                LocalDateTime.of(2021, 9, 16, 13, 0, 0)
        );
    }

    @Test
    void itShouldReturnFailureWhenProposedScheduleTimeHasConflictsWithExistentBookingsForPrivateOffice() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new PrivateOffice(10));
        var booking14To15 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timeZoneARG
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        timeZoneARG
                )).build();
        var booking9To12 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        9, 0, 0, 0,
                        timeZoneARG
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        timeZoneARG
                )).build();

        Either<UseCaseError, Booking> bookingOrError = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(2018, 12, 8, 9, 0, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")),
                ZonedDateTime.of(2018, 12, 8, 13, 0, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")),
                ImmutableList.of(booking9To12, booking14To15)
        );

        assertThat(bookingOrError.isLeft()).isTrue();
        assertThat(bookingOrError.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }

    @Test
    void itShouldReturnFailureWhenProposedScheduleTimeHasConflictsWithExistentBookingsForSharedOffice() {
        var office = Office.create(new OfficeId(), "123", "MM", 10, new SharedOffice(3, 10));
        var booking14To15_1 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timeZoneARG
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        timeZoneARG
                )).build();
        var booking14To15_2 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timeZoneARG
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        timeZoneARG
                )).build();
        var booking14To15_3 = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timeZoneARG
                ))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        timeZoneARG
                )).build();

        Either<UseCaseError, Booking> bookingOrError = office.book(
                "test@email.com",
                10,
                ZonedDateTime.of(
                        2018, 12, 8,
                        12, 0, 0, 0,
                        timeZoneARG
                ),
                ZonedDateTime.of(
                        2018, 12, 8,
                        15, 0, 0, 0,
                        timeZoneARG
                ),
                ImmutableList.of(booking14To15_1, booking14To15_2, booking14To15_3)
        );

        assertThat(bookingOrError.isLeft()).isTrue();
        assertThat(bookingOrError.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }
}
