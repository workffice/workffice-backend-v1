package booking.infrastructure.mercadopago;

import backoffice.application.office_branch.OfficeBranchFinder;
import booking.application.booking.BookingEmailNotificator;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.PaymentInformation;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.factories.MembershipAcquisitionBuilder;
import com.mercadopago.MercadoPago;
import com.mercadopago.exceptions.MPConfException;
import com.mercadopago.resources.Payment;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.domain.EventBus;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMercadoPagoPaymentResolverMembership {
    Environment env = mock(Environment.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    BookingEmailNotificator emailNotificator = mock(BookingEmailNotificator.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    EventBus eventBus = mock(EventBus.class);
    ArgumentCaptor<MembershipAcquisition> membershipAcquisitionCaptor = ArgumentCaptor
            .forClass(MembershipAcquisition.class);

    MercadoPagoPaymentResolver mercadoPagoPaymentResolver = new MercadoPagoPaymentResolver(
            env,
            bookingRepo,
            membershipAcquisitionRepo,
            emailNotificator,
            officeBranchFinder,
            eventBus
    );

    MercadoPagoPaymentNotification notification = MercadoPagoPaymentNotification.of(
            "12345",
            "true",
            "payment",
            "2015-03-25T10:04:58.396-04:00",
            "123123123",
            "44444",
            "1",
            "v1",
            "payment.created",
            MercadoPagoPaymentNotification.Data.of("1")
    );

    @BeforeEach
    void beforeEach() {
        when(env.getProperty("MERCADO_PAGO_ACCESS_TOKEN")).thenReturn("test-1234");
    }

    @Test
    void itShouldNotUpdateMembershipAcquisitionWhenMercadoPagoSDKCannotInitialize() {
        try (MockedStatic<MercadoPago.SDK> mpSDKMock = mockStatic(MercadoPago.SDK.class)) {
            mpSDKMock.when(() -> MercadoPago.SDK.setAccessToken("test-1234"))
                    .thenThrow(new MPConfException("Boom!"));

            mercadoPagoPaymentResolver
                    .handleNotificationForMembershipAcquisition("12", notification);

            verify(membershipAcquisitionRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateMembershipAcquisitionWhenPaymentCannotBeRetrieved() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(null);

            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition("12", notification);

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateMembershipAcquisitionWhenItDoesNotExist() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var id = new MembershipAcquisitionId();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment());
            when(membershipAcquisitionRepo.findById(id)).thenReturn(Option.none());

            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition(id.toString(), notification);

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateMembershipAcquisitionWhenItIsNotPending() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var membershipAcquisition = new MembershipAcquisitionBuilder().build();
            var paymentInformation = new PaymentInformation(
                    "1",
                    100f,
                    0f,
                    "ARS",
                    "visa",
                    "credit_card"
            );
            membershipAcquisition.buy(paymentInformation);
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment());
            when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                    .thenReturn(Option.of(membershipAcquisition));

            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition(
                    membershipAcquisition.id().toString(),
                    notification
            );

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "pending",
            "authorized",
            "in_process",
            "in_mediation",
    })
    void itShouldNotUpdateMembershipAcquisitionWhenPaymentStatusIsPending(
            String paymentStatus
    ) {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var membershipAcquisition = new MembershipAcquisitionBuilder().build();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment().setStatus(Payment.Status.valueOf(paymentStatus)));
            when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                    .thenReturn(Option.of(membershipAcquisition));

            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition(
                    membershipAcquisition.id().toString(),
                    notification
            );

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldUpdateMembershipAcquisitionToBoughtStatus() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var membershipAcquisition = new MembershipAcquisitionBuilder().build();
            Payment payment = Mockito.mock(Payment.class);
            when(payment.getTransactionAmount()).thenReturn(110f);
            when(payment.getStatus()).thenReturn(Payment.Status.approved);
            when(payment.getFeeDetails()).thenReturn(new ArrayList<>());
            when(payment.getCurrencyId()).thenReturn(Payment.CurrencyId.ARS);
            when(payment.getPaymentTypeId()).thenReturn(Payment.PaymentTypeId.debit_card);

            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId())).thenReturn(payment);
            when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                    .thenReturn(Option.of(membershipAcquisition));
            when(membershipAcquisitionRepo.update(any())).thenReturn(Try.success(null));

            mercadoPagoPaymentResolver.handleNotificationForMembershipAcquisition(
                    membershipAcquisition.id().toString(),
                    notification
            );

            verify(membershipAcquisitionRepo, times(1))
                    .update(membershipAcquisitionCaptor.capture());
            var membershipAcquisitionUpdated = membershipAcquisitionCaptor.getValue();
            assertThat(membershipAcquisitionUpdated.isBought()).isTrue();
        }
    }
}
