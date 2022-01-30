package booking.infrastructure.mercadopago;

import booking.application.dto.booking.BookingError;
import booking.application.dto.booking.BookingPreferenceInformation;
import booking.domain.payment_preference.PaymentPreference;
import com.mercadopago.MercadoPago;
import com.mercadopago.core.MPApiResponse;
import com.mercadopago.exceptions.MPConfException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.Preference;
import io.vavr.control.Either;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMercadoPagoPreferenceCreator {
    Environment env = mock(Environment.class);
    Preference mockPreference = mock(Preference.class);
    MercadoPagoPreferenceCreator mpPreferenceCreator = new MercadoPagoPreferenceCreator(env);

    @BeforeEach
    public void beforeEach() {
        mpPreferenceCreator.setPreference(mockPreference);
    }

    @Test
    void itShouldReturnMercadoPagoErrorWhenSDKCannotBeInitialized() {
        try (MockedStatic<MercadoPago.SDK> mpSDK = Mockito.mockStatic(MercadoPago.SDK.class)) {
            when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
            mpSDK.when(() -> MercadoPago.SDK.setAccessToken("test-1234")).thenThrow(new MPConfException("boom!"));

            var info = BookingPreferenceInformation.of(
                    "1",
                    "john@doe.com",
                    "Bla",
                    10f,
                    "Some description"
            );
            Either<BookingError, PaymentPreference> response = mpPreferenceCreator.createForBooking(info);
            assertThat(response.isLeft()).isTrue();
            assertThat(response.getLeft()).isEqualTo(BookingError.MERCADO_PAGO_ERROR);
        }
    }

    @Test
    void itShouldReturnMercadoPagoErrorWhenPreferenceSaveFails() throws MPException {
        when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
        when(mockPreference.save()).thenThrow(new MPException("Boom!"));

        var info = BookingPreferenceInformation.of(
                "1",
                "john@doe.com",
                "Bla",
                10f,
                "Some description"
        );
        Either<BookingError, PaymentPreference> response = mpPreferenceCreator.createForBooking(info);
        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(BookingError.MERCADO_PAGO_ERROR);
    }

    @Test
    void itShouldReturnPreferenceIdGenerated() {
        when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
        when(env.getProperty("SERVER_HOST")).thenReturn("http://localhost:8080");
        when(mockPreference.getId()).thenReturn("123");
        MPApiResponse mockMPApiResponse = mock(MPApiResponse.class);
        when(mockPreference.getLastApiResponse()).thenReturn(mockMPApiResponse);
        when(mockMPApiResponse.getStringResponse()).thenReturn("Mock response");

        var info = BookingPreferenceInformation.of(
                "1",
                "john@doe.com",
                "Bla",
                10f,
                "Some description"
        );
        Either<BookingError, PaymentPreference> response = mpPreferenceCreator.createForBooking(info);
        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(PaymentPreference.of("123"));
        verify(mockPreference, times(1)).setNotificationUrl(
                "http://localhost:8080/api/bookings/1/mp_notifications/?source_news=webhooks"
        );
    }
}
