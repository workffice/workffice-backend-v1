package booking.application.membership_acquisition;

import authentication.application.AuthUserFinder;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.application.dto.membership_acquisition.MembershipAcquisitionResponse;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import io.vavr.control.Either;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static booking.application.dto.membership_acquisition.MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN;

@Service
public class MembershipAcquisitionFinder {
    private final MembershipAcquisitionRepository membershipAcquisitionRepo;
    private final AuthUserFinder                  authUserFinder;

    public MembershipAcquisitionFinder(
            MembershipAcquisitionRepository membershipAcquisitionRepo,
            AuthUserFinder                  authUserFinder
    ) {
        this.membershipAcquisitionRepo = membershipAcquisitionRepo;
        this.authUserFinder            = authUserFinder;
    }

    public Either<MembershipAcquisitionError, List<MembershipAcquisitionResponse>> find() {
        return authUserFinder
                .findAuthenticatedUser()
                .toEither(MEMBERSHIP_ACQUISITION_FORBIDDEN)
                .map(authUser -> membershipAcquisitionRepo.find(authUser.getEmail()))
                .map(membershipAcquisitions -> membershipAcquisitions
                        .stream()
                        .map(MembershipAcquisition::toResponse)
                        .collect(Collectors.toList()));
    }
}
