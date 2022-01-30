package booking.application.booking;

import authentication.application.AuthUserValidator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingResponse;
import booking.domain.booking.BookingRepository;
import booking.factories.BookingBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBookingFinderByRenter {
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);

    BookingFinder finder = new BookingFinder(bookingRepo, authUserValidator);

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToBooking() {
        var booking = new BookingBuilder().build();
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(false);

        Either<BookingError, Page<BookingResponse>> response = finder.find(
                "napoleon@mail.com",
                true,
                PageRequest.of(1, 20)
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_FORBIDDEN);
    }

    @Test
    void itShouldReturnBookingResponse() {
        var booking = new BookingBuilder().build();
        var booking2 = new BookingBuilder().build();
        when(bookingRepo.find(
                "napoleon@mail.com",
                true,
                LocalDate.now(Clock.systemUTC()),
                0,
                2
        )).thenReturn(ImmutableList.of(booking, booking2));
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@mail.com")).thenReturn(true);
        when(bookingRepo.count(
                "napoleon@mail.com",
                true,
                LocalDate.now(Clock.systemUTC())
        )).thenReturn(4L);

        Either<BookingError, Page<BookingResponse>> response = finder.find(
                "napoleon@mail.com",
                true,
                PageRequest.of(0, 2)
        );

        assertThat(response.isRight()).isTrue();
        assertThat(response.get().getTotalPages()).isEqualTo(2);
        assertThat(response.get().getContent()).containsExactly(
                booking.toResponse(),
                booking2.toResponse()
        );
    }
}
