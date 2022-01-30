package booking.application.membership_acquisition;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.AuthUserResponse;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import booking.factories.MembershipAcquisitionBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMembershipAcquisitionFinder {
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    AuthUserFinder authUserFinder = mock(AuthUserFinder.class);

    MembershipAcquisitionFinder finder = new MembershipAcquisitionFinder(
            membershipAcquisitionRepo,
            authUserFinder
    );

    @Test
    void itShouldReturnForbiddenWhenThereIsNoAuthenticatedUser() {
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.none());

        var response = finder.find();

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_ACQUISITION_FORBIDDEN);
    }

    @Test
    void itShouldReturnMembershipAcquisitions() {
        var authUser = AuthUserResponse.of(
                "",
                "napoleon@mail.com",
                "",
                "",
                "",
                "",
                "",
                ""
        );
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUser));
        var membership = new MembershipAcquisitionBuilder().build();
        var membership2 = new MembershipAcquisitionBuilder().build();
        when(membershipAcquisitionRepo.find("napoleon@mail.com"))
                .thenReturn(ImmutableList.of(membership, membership2));

        var response = finder.find();

        assertThat(response.isRight()).isTrue();
        assertThat(response.get()).containsExactlyInAnyOrder(
                membership.toResponse(),
                membership2.toResponse()
        );
    }
}
