package booking.application.booking.booking_creation;

import booking.application.booking.creation.SingleBookingCreator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingInformation;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.Status;
import booking.domain.office.Office;
import booking.domain.office.privacy.PrivateOffice;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSingleBookingCreator {
    ZoneId timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");
    BookingRepository bookingRepo = mock(BookingRepository.class);

    SingleBookingCreator creator = new SingleBookingCreator(bookingRepo);

    @Test
    void itShouldReturnInvalidScheduleTimeWhenScheduleTimeProvidedIsInvalid() {
        var office = new OfficeBuilder().build();
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 14, 0, 0),
                LocalDateTime.of(2018, 12, 8, 12, 0, 0)
        );

        Either<UseCaseError, Booking> response = creator.book(office, "john@doe.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.INVALID_SCHEDULE_TIME);
    }

    @Test
    void itShouldReturnOfficeIsNotAvailableWhenOfficeIsUnavailableOrThereIsABookingConflict() {
        var office = new OfficeBuilder()
                .withPrivacy(new PrivateOffice(1))
                .build();
        var existentBooking = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .build();
        when(bookingRepo.find(any(), any())).thenReturn(ImmutableList.of(existentBooking));
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 14, 0, 0),
                LocalDateTime.of(2018, 12, 8, 15, 0, 0)
        );

        Either<UseCaseError, Booking> response = creator.book(office, "john@wick.com", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.OFFICE_IS_NOT_AVAILABLE);
    }

    @Test
    void itShouldReturnBookingCreated() {
        var office = new OfficeBuilder()
                .withPrivacy(new PrivateOffice(1))
                .build();
        var existentBooking = new BookingBuilder()
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timezoneARG))
                .withStatus(Status.SCHEDULED)
                .build();
        when(bookingRepo.find(any(Office.class), any(LocalDate.class))).thenReturn(ImmutableList.of(existentBooking));
        when(bookingRepo.store(any())).thenReturn(Try.success(null));
        BookingInformation info = BookingInformation.of(
                10,
                LocalDateTime.of(2018, 12, 8, 15, 0, 0),
                LocalDateTime.of(2018, 12, 8, 16, 0, 0)
        );

        Either<UseCaseError, Booking> response = creator.book(office, "john@wick.com", info);

        assertThat(response.isRight()).isTrue();
        var booking = response.get();
        assertThat(booking.toResponse()).isEqualTo(BookingResponse.of(
                booking.id().toString(),
                Status.PENDING.name(),
                10,
                office.price(),
                booking.toResponse().getCreated(),
                LocalDateTime.of(2018, 12, 8, 15, 0, 0),
                LocalDateTime.of(2018, 12, 8, 16, 0, 0),
                null,
                office.id().toString(),
                office.name(),
                office.officeBranchId()
        ));
    }
}
