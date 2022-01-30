package booking.application.booking;

import booking.application.dto.OfficeError;
import booking.application.dto.booking.BookingScheduleTimeResponse;
import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingScheduleTimeFinder {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingScheduleTimeFinder finder = new BookingScheduleTimeFinder(officeRepo, bookingRepo);

    @Test
    void itShouldReturnNotFoundWhenOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        Either<OfficeError, List<BookingScheduleTimeResponse>> response = finder.findBookingScheduledTimes(
                officeId,
                LocalDate.of(2018, 12, 8)
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeError.OFFICE_NOT_FOUND);
    }

    @Test
    void itShouldReturnBookingScheduleTimeForOfficeAndScheduleDateSpecified() {
        var office = new OfficeBuilder().build();
        var booking1 = new BookingBuilder().withOffice(office).build();
        var booking2 = new BookingBuilder().withOffice(office).build();
        var booking3 = new BookingBuilder().withOffice(office).build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(bookingRepo.find(any(Office.class), eq(LocalDate.of(2018, 12, 8))))
                .thenReturn(ImmutableList.of(
                        booking1,
                        booking2,
                        booking3
                ));

        Either<OfficeError, List<BookingScheduleTimeResponse>> response = finder.findBookingScheduledTimes(
                office.id(),
                LocalDate.of(2018, 12, 8)
        );

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactlyInAnyOrder(
                booking1.toScheduleTimeResponse(),
                booking2.toScheduleTimeResponse(),
                booking3.toScheduleTimeResponse()
        );
    }
}
