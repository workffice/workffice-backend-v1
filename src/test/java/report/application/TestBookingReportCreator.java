package report.application;

import booking.domain.booking.BookingConfirmedEvent;
import io.vavr.control.Try;
import report.domain.Booking;
import report.domain.BookingRepository;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestBookingReportCreator {
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingReportCreator creator = new BookingReportCreator(bookingRepo);

    @Test
    void itShouldStoreBookingReportingWithInformationSpecified() {
        when(bookingRepo.store(any())).thenReturn(Try.success(null));
        var event = BookingConfirmedEvent.of(
                "123",
                "1",
                "30",
                500f,
                LocalDate.of(2018, 12, 8),
                "pepito@mail.com"
        );

        creator.create(event);

        verify(bookingRepo, times(1)).store(Booking.create(
                "123",
                "1",
                "30",
                500f,
                LocalDate.of(2018, 12, 8)
        ));
    }
}
