package booking.application.membership_acquisition;

import authentication.application.AuthUserFinder;
import backoffice.application.dto.membership.MembershipResponse;
import backoffice.application.membership.MembershipFinder;
import backoffice.domain.membership.MembershipId;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import io.vavr.Function2;
import io.vavr.control.Either;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.DB_ERROR;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_NOT_FOUND;
import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.NO_AUTHENTICATED_USER;

@Service
public class MembershipAcquisitionCreator {
    private final AuthUserFinder                  authUserFinder;
    private final MembershipAcquisitionRepository membershipAcquisitionRepo;
    private final MembershipFinder                membershipFinder;

    public MembershipAcquisitionCreator(
            AuthUserFinder                  authUserFinder,
            MembershipAcquisitionRepository membershipAcquisitionRepo,
            MembershipFinder                membershipFinder
    ) {
        this.authUserFinder            = authUserFinder;
        this.membershipAcquisitionRepo = membershipAcquisitionRepo;
        this.membershipFinder          = membershipFinder;
    }

    private Function2<String, MembershipResponse, MembershipAcquisition> bookMembership(MembershipAcquisitionId id) {
        return (buyerEmail, membershipResponse) -> MembershipAcquisition.create(
                id,
                membershipResponse.getId(),
                buyerEmail,
                membershipResponse.getPricePerMonth(),
                LocalDate.now(Clock.systemUTC()).getMonth(),
                membershipResponse.getAccessDays().stream().map(DayOfWeek::valueOf).collect(Collectors.toSet())
        );
    }

    public Either<MembershipAcquisitionError, Void> create(MembershipAcquisitionId id, String membershipId) {
        return authUserFinder
                .findAuthenticatedUser()
                .toEither(NO_AUTHENTICATED_USER)
                .map(authUser -> bookMembership(id).curried().apply(authUser.getEmail()))
                .flatMap(bookMembership -> membershipFinder
                        .findById(MembershipId.fromString(membershipId))
                        .map(bookMembership)
                        .toEither(MEMBERSHIP_NOT_FOUND))
                .flatMap(membershipBooking -> membershipAcquisitionRepo.store(membershipBooking).toEither(DB_ERROR));
    }
}
