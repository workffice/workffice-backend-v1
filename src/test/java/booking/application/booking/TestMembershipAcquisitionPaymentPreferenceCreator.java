package booking.application.booking;

import authentication.application.AuthUserValidator;
import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingPreferenceInformation;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.Status;
import booking.domain.payment_preference.PaymentPreference;
import booking.domain.payment_preference.PreferenceCreator;
import booking.factories.BookingBuilder;
import booking.factories.OfficeBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipAcquisitionPaymentPreferenceCreator {
    BookingRepository bookingRepo = mock(BookingRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    PreferenceCreator preferenceCreator = mock(PreferenceCreator.class);

    PaymentPreferenceCreator paymentPreferenceCreator = new PaymentPreferenceCreator(
            bookingRepo,
            authUserValidator,
            preferenceCreator
    );

    @Test
    void itShouldReturnBookingNotFoundWhenBookingDoesNotExist() {
        var bookingId = new BookingId();
        when(bookingRepo.findById(bookingId)).thenReturn(Option.none());

        Either<BookingError, PaymentPreference> response = paymentPreferenceCreator.create(bookingId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserIsNotTheRenterOfTheBooking() {
        var booking = new BookingBuilder().build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(false);

        Either<BookingError, PaymentPreference> response = paymentPreferenceCreator.create(booking.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_FORBIDDEN);
    }

    @Test
    void itShouldReturnBookingAlreadyScheduleWhenBookingIsInScheduledStatus() {
        var booking = new BookingBuilder()
                .withStatus(Status.SCHEDULED)
                .build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(true);

        Either<BookingError, PaymentPreference> response = paymentPreferenceCreator.create(booking.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.BOOKING_IS_NOT_PENDING);
    }

    @Test
    void itShouldReturnMercadoPagoErrorWhenMercadoPagoPreferenceCreatorFails() {
        var booking = new BookingBuilder().build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(true);
        when(preferenceCreator.createForBooking(any(BookingPreferenceInformation.class)))
                .thenReturn(Either.left(BookingError.MERCADO_PAGO_ERROR));

        Either<BookingError, PaymentPreference> response = paymentPreferenceCreator.create(booking.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.MERCADO_PAGO_ERROR);
    }

    @Test
    void itShouldReturnPreferenceIdCreated() {
        var timezoneARG = ZoneId.of("America/Argentina/Buenos_Aires");
        var office = new OfficeBuilder()
                .withPrice(400)
                .build();
        var booking = new BookingBuilder()
                .withOffice(office)
                .withStartTime(ZonedDateTime.of(
                        2018, 12, 8,
                        14, 0, 0, 0,
                        timezoneARG))
                .withEndTime(ZonedDateTime.of(
                        2018, 12, 8,
                        18, 0, 0, 0,
                        timezoneARG))
                .build();
        when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
        when(authUserValidator.isSameUserAsAuthenticated(booking.renterEmail())).thenReturn(true);
        ArgumentCaptor<BookingPreferenceInformation> preferenceInformationArgumentCaptor = ArgumentCaptor
                .forClass(BookingPreferenceInformation.class);
        when(preferenceCreator.createForBooking(any(BookingPreferenceInformation.class)))
                .thenReturn(Either.right(PaymentPreference.of("123")));

        Either<BookingError, PaymentPreference> response = paymentPreferenceCreator.create(booking.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(PaymentPreference.of("123"));
        verify(preferenceCreator, times(1)).createForBooking(preferenceInformationArgumentCaptor.capture());
        var preferenceInformation = preferenceInformationArgumentCaptor.getValue();
        // 4 hours of renting with a base price of 400
        assertThat(preferenceInformation.getPrice()).isEqualTo(1600f);
    }
}
