package booking.application.booking;

import booking.domain.booking.BookingRepository;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import booking.factories.OfficeBuilder;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingExistsResolver {
    OfficeRepository officeRepo = mock(OfficeRepository.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingExistsResolver bookingExistsResolver = new BookingExistsResolver(officeRepo, bookingRepo);

    @Test
    void itShouldReturnFalseIfOfficeDoesNotExist() {
        var officeId = new OfficeId();
        when(officeRepo.findById(officeId)).thenReturn(Option.none());

        boolean exists = bookingExistsResolver.bookingExists("some@email.com", officeId);

        assertThat(exists).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenRenterHasNoBookingsForOfficeSpecified() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(bookingRepo.exists(eq("some@email.com"), any(Office.class))).thenReturn(false);

        boolean exists = bookingExistsResolver.bookingExists("some@email.com", office.id());

        assertThat(exists).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenRenterHasBookingsForOfficeSpecified() {
        var office = new OfficeBuilder().build();
        when(officeRepo.findById(office.id())).thenReturn(Option.of(office));
        when(bookingRepo.exists(eq("some@email.com"), any(Office.class))).thenReturn(true);

        boolean exists = bookingExistsResolver.bookingExists("some@email.com", office.id());

        assertThat(exists).isTrue();
    }
}
