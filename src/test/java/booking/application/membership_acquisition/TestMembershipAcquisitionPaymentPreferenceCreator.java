package booking.application.membership_acquisition;

import authentication.application.AuthUserValidator;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.application.dto.membership_acquisition.MembershipAcquisitionPreference;
import booking.domain.booking.PaymentInformation;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.domain.payment_preference.PaymentPreference;
import booking.domain.payment_preference.PreferenceCreator;
import booking.factories.MembershipAcquisitionBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipAcquisitionPaymentPreferenceCreator {
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    PreferenceCreator preferenceCreator = mock(PreferenceCreator.class);

    MembershipAcquisitionPaymentPreferenceCreator creator =
            new MembershipAcquisitionPaymentPreferenceCreator(
                    membershipAcquisitionRepo,
                    authUserValidator,
                    preferenceCreator
            );

    @Test
    void itShouldReturnMembershipAcquisitionNotFoundWhenItDoesNotExist() {
        var id = new MembershipAcquisitionId();
        when(membershipAcquisitionRepo.findById(id)).thenReturn(Option.none());

        Either<MembershipAcquisitionError, PaymentPreference> response = creator.create(id);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserIsNotTheRenterOfTheBooking() {
        var membershipAcquisition = new MembershipAcquisitionBuilder().build();
        when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                .thenReturn(Option.of(membershipAcquisition));
        when(authUserValidator.isSameUserAsAuthenticated(membershipAcquisition.buyerEmail())).thenReturn(false);

        Either<MembershipAcquisitionError, PaymentPreference> response =
                creator.create(membershipAcquisition.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN);
    }

    @Test
    void itShouldReturnBookingAlreadyScheduleWhenBookingIsInScheduledStatus() {
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
        when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                .thenReturn(Option.of(membershipAcquisition));
        when(authUserValidator.isSameUserAsAuthenticated(membershipAcquisition.buyerEmail())).thenReturn(true);

        Either<MembershipAcquisitionError, PaymentPreference> response =
                creator.create(membershipAcquisition.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_PENDING);
    }

    @Test
    void itShouldReturnMercadoPagoErrorWhenMercadoPagoPreferenceCreatorFails() {
        var membershipAcquisition = new MembershipAcquisitionBuilder().build();
        when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                .thenReturn(Option.of(membershipAcquisition));
        when(authUserValidator.isSameUserAsAuthenticated(membershipAcquisition.buyerEmail())).thenReturn(true);
        when(preferenceCreator.createForMembership(any(MembershipAcquisitionPreference.class)))
                .thenReturn(Either.left(MembershipAcquisitionError.MERCADO_PAGO_ERROR));

        Either<MembershipAcquisitionError, PaymentPreference> response =
                creator.create(membershipAcquisition.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MERCADO_PAGO_ERROR);
    }

    @Test
    void itShouldReturnPreferenceIdCreated() {
        var membershipAcquisition = new MembershipAcquisitionBuilder()
                .withPrice(1500)
                .build();
        when(membershipAcquisitionRepo.findById(membershipAcquisition.id()))
                .thenReturn(Option.of(membershipAcquisition));
        when(authUserValidator.isSameUserAsAuthenticated(membershipAcquisition.buyerEmail())).thenReturn(true);
        when(preferenceCreator.createForMembership(any(MembershipAcquisitionPreference.class)))
                .thenReturn(Either.right(PaymentPreference.of("123")));

        Either<MembershipAcquisitionError, PaymentPreference> response =
                creator.create(membershipAcquisition.id());

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).isEqualTo(PaymentPreference.of("123"));
        verify(preferenceCreator, times(1)).createForMembership(membershipAcquisition.createPaymentPreference());
    }
}
