package booking.infrastructure.mercadopago;

import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.factories.OfficeBranchBuilder;
import booking.application.booking.BookingEmailNotificator;
import booking.domain.booking.Booking;
import booking.domain.booking.BookingConfirmedEvent;
import booking.domain.booking.BookingId;
import booking.domain.booking.BookingRepository;
import booking.domain.booking.Status;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.factories.BookingBuilder;
import com.mercadopago.MercadoPago;
import com.mercadopago.exceptions.MPConfException;
import com.mercadopago.resources.Payment;
import io.vavr.control.Option;
import io.vavr.control.Try;
import shared.domain.EventBus;

import java.time.Clock;
import java.time.LocalDate;
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

public class TestMercadoPagoPaymentResolver {

    Environment env = mock(Environment.class);
    BookingRepository bookingRepo = mock(BookingRepository.class);
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    BookingEmailNotificator emailNotificator = mock(BookingEmailNotificator.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    EventBus eventBus = mock(EventBus.class);
    ArgumentCaptor<Booking> bookingArgumentCaptor = ArgumentCaptor.forClass(Booking.class);

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
    void itShouldNotUpdateBookingWhenMercadoPagoSDKCannotInitialize() {
        try (MockedStatic<MercadoPago.SDK> mpSDKMock = mockStatic(MercadoPago.SDK.class)) {
            mpSDKMock.when(() -> MercadoPago.SDK.setAccessToken("test-1234"))
                    .thenThrow(new MPConfException("Boom!"));

            mercadoPagoPaymentResolver.handleNotification("12", notification);

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateBookingWhenPaymentCannotBeRetrieved() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(null);

            mercadoPagoPaymentResolver.handleNotification("12", notification);

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateBookingWhenBookingDoesNotExist() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var bookingId = new BookingId();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment());
            when(bookingRepo.findById(bookingId)).thenReturn(Option.none());

            mercadoPagoPaymentResolver.handleNotification(bookingId.toString(), notification);

            verify(bookingRepo, times(0)).update(any());
        }
    }

    @Test
    void itShouldNotUpdateBookingWhenItIsNotPending() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var booking = new BookingBuilder()
                    .withStatus(Status.SCHEDULED)
                    .build();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment());
            when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));

            mercadoPagoPaymentResolver.handleNotification(booking.id().toString(), notification);

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
    void itShouldNotUpdateBookingWhenPaymentStatusIsPending(String paymentStatus) {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var booking = new BookingBuilder().build();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment().setStatus(Payment.Status.valueOf(paymentStatus)));
            when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));

            mercadoPagoPaymentResolver.handleNotification(booking.id().toString(), notification);

            verify(bookingRepo, times(0)).update(any());
            verify(emailNotificator, times(0)).sendBookingPaymentFailedEmail(booking.renterEmail());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rejected",
            "cancelled",
            "refunded",
            "charged_back"
    })
    void itShouldSendNotificationEmailWhenPaymentStatusIsNotSuccess(String paymentStatus) {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var booking = new BookingBuilder().build();
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId()))
                    .thenReturn(new Payment().setStatus(Payment.Status.valueOf(paymentStatus)));
            when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));

            mercadoPagoPaymentResolver.handleNotification(booking.id().toString(), notification);

            verify(bookingRepo, times(0)).update(any());
            verify(emailNotificator, times(1)).sendBookingPaymentFailedEmail(booking.renterEmail());
        }
    }

    @Test
    void itShouldUpdateBookingToScheduledStatus() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var booking = new BookingBuilder().build();
            var officeBranchExample = new OfficeBranchBuilder()
                    .withId(OfficeBranchId.fromString(booking.office().officeBranchId()))
                    .build();
            Payment payment = Mockito.mock(Payment.class);
            when(payment.getTransactionAmount()).thenReturn(110f);
            when(payment.getStatus()).thenReturn(Payment.Status.approved);
            when(payment.getFeeDetails()).thenReturn(new ArrayList<>());
            when(payment.getCurrencyId()).thenReturn(Payment.CurrencyId.ARS);
            when(payment.getPaymentTypeId()).thenReturn(Payment.PaymentTypeId.debit_card);

            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId())).thenReturn(payment);
            when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
            when(officeBranchFinder.find(officeBranchExample.id()))
                    .thenReturn(Option.of(officeBranchExample.toResponse()));
            when(bookingRepo.update(any())).thenReturn(Try.success(null));

            mercadoPagoPaymentResolver.handleNotification(booking.id().toString(), notification);

            verify(bookingRepo, times(1)).update(bookingArgumentCaptor.capture());
            verify(emailNotificator, times(1)).sendBookingPaymentAcceptedEmail(
                    booking.renterEmail(),
                    booking.id(),
                    booking.office().name(),
                    booking.startScheduleTime(),
                    booking.endScheduleTime(),
                    booking.amountOfHours(),
                    110f,
                    officeBranchExample.toResponse().getLocation()
            );
            var bookingUpdated = bookingArgumentCaptor.getValue();
            assertThat(bookingUpdated.isScheduled()).isTrue();
        }
    }

    @Test
    void itShouldPublishBookingConfirmedEvent() {
        try (MockedStatic<Payment> mpPaymentMock = mockStatic(Payment.class)) {
            var booking = new BookingBuilder().build();
            var officeBranchExample = new OfficeBranchBuilder()
                    .withId(OfficeBranchId.fromString(booking.office().officeBranchId()))
                    .build();
            Payment payment = Mockito.mock(Payment.class);
            when(payment.getTransactionAmount()).thenReturn(110f);
            when(payment.getStatus()).thenReturn(Payment.Status.approved);
            when(payment.getFeeDetails()).thenReturn(new ArrayList<>());
            when(payment.getCurrencyId()).thenReturn(Payment.CurrencyId.ARS);
            when(payment.getPaymentTypeId()).thenReturn(Payment.PaymentTypeId.debit_card);

            when(bookingRepo.findById(booking.id())).thenReturn(Option.of(booking));
            mpPaymentMock.when(() -> Payment.findById(notification.getData().getId())).thenReturn(payment);
            when(officeBranchFinder.find(officeBranchExample.id()))
                    .thenReturn(Option.of(officeBranchExample.toResponse()));
            when(bookingRepo.update(any())).thenReturn(Try.success(null));

            mercadoPagoPaymentResolver.handleNotification(booking.id().toString(), notification);

            verify(eventBus, times(1)).publish(BookingConfirmedEvent.of(
                    booking.id().toString(),
                    booking.office().officeBranchId(),
                    booking.office().id().toString(),
                    110f,
                    LocalDate.now(Clock.systemUTC()),
                    booking.renterEmail()
            ));
        }
    }
}
