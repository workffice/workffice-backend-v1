package booking.application.booking;

import authentication.application.AuthUserValidator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.factories.BookingBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingFinder {
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingFinder finder = new BookingFinder(bookingRepo, authUserValidator);

    @Test
    void itShouldReturnNotFoundWhenThereIsNoBookingWithIdProvided() {
        var bookingId = new BookingId();
        when(bookingRepo.findById(bookingId)).thenReturn(Option.none());

        Either<BookingError, BookingResponse> response = finder.find(bookingId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToBooking() {
        var booking = new BookingBuilder().build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(false);

        Either<BookingError, BookingResponse> response = finder.find(booking.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_FORBIDDEN);
    }

    @Test
    void itShouldReturnBookingResponse() {
        var booking = new BookingBuilder().build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(true);

        Either<BookingError, BookingResponse> response = finder.find(booking.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(booking.toResponse());
    }
}
