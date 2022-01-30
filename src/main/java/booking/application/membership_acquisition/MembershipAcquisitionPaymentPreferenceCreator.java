package booking.application.membership_acquisition;

import authentication.application.AuthUserValidator;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.domain.payment_preference.PaymentPreference;
import booking.domain.payment_preference.PreferenceCreator;
import io.vavr.control.Either;

import org.springframework.stereotype.Service;

import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_IS_NOT_PENDING;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_NOT_FOUND;

@Service
public class MembershipAcquisitionPaymentPreferenceCreator {
    private final MembershipAcquisitionRepository membershipAcquisitionRepo;
    private final AuthUserValidator               authUserValidator;
    private final PreferenceCreator               preferenceCreator;

    public MembershipAcquisitionPaymentPreferenceCreator(
            MembershipAcquisitionRepository membershipAcquisitionRepo,
            AuthUserValidator               authUserValidator,
            PreferenceCreator               preferenceCreator
    ) {
        this.membershipAcquisitionRepo = membershipAcquisitionRepo;
        this.authUserValidator         = authUserValidator;
        this.preferenceCreator         = preferenceCreator;
    }

    public Either<MembershipAcquisitionError, PaymentPreference> create(MembershipAcquisitionId id) {
        return membershipAcquisitionRepo.findById(id)
                .toEither(MEMBERSHIP_ACQUISITION_NOT_FOUND)
                .filterOrElse(
                        membershipAcquisition -> authUserValidator.isSameUserAsAuthenticated(
                                membershipAcquisition.buyerEmail()
                        ),
                        m -> MEMBERSHIP_ACQUISITION_FORBIDDEN)
                .filterOrElse(
                        MembershipAcquisition::isPending,
                        b -> MEMBERSHIP_ACQUISITION_IS_NOT_PENDING)
                .flatMap(membershipAcquisition -> preferenceCreator
                        .createForMembership(membershipAcquisition.createPaymentPreference()));
    }
}
