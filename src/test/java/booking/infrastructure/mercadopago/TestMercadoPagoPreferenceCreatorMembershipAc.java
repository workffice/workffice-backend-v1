package booking.infrastructure.mercadopago;

import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.application.dto.membership_acquisition.MembershipAcquisitionPreference;
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

public class TestMercadoPagoPreferenceCreatorMembershipAc {
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

            var info = MembershipAcquisitionPreference.of(
                    "1",
                    "Bla",
                    "john@doe.com",
                    10f,
                    "Some description"
            );
            Either<MembershipAcquisitionError, PaymentPreference> response = mpPreferenceCreator
                    .createForMembership(info);
            assertThat(response.isLeft()).isTrue();
            assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MERCADO_PAGO_ERROR);
        }
    }

    @Test
    void itShouldReturnMercadoPagoErrorWhenPreferenceSaveFails() throws MPException {
        when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
        when(mockPreference.save()).thenThrow(new MPException("Boom!"));

        var info = MembershipAcquisitionPreference.of(
                "1",
                "Bla",
                "john@doe.com",
                10f,
                "Some description"
        );
        Either<MembershipAcquisitionError, PaymentPreference> response = mpPreferenceCreator.createForMembership(info);
        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MERCADO_PAGO_ERROR);
    }

    @Test
    void itShouldReturnPreferenceIdGenerated() {
        when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
        when(env.getProperty("SERVER_HOST")).thenReturn("http://localhost:8080");
        when(mockPreference.getId()).thenReturn("123");
        MPApiResponse mockMPApiResponse = mock(MPApiResponse.class);
        when(mockPreference.getLastApiResponse()).thenReturn(mockMPApiResponse);
        when(mockMPApiResponse.getStringResponse()).thenReturn("Mock response");

        var info = MembershipAcquisitionPreference.of(
                "1",
                "Bla",
                "john@doe.com",
                10f,
                "Some description"
        );
        Either<MembershipAcquisitionError, PaymentPreference> response = mpPreferenceCreator.createForMembership(info);
        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(PaymentPreference.of("123"));
        verify(mockPreference, times(1)).setNotificationUrl(
                "http://localhost:8080/api/membership_acquisitions/1/mp_notifications/?source_news=webhooks"
        );
    }
}
