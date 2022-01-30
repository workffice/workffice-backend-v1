package booking.application.membership_acquisition;

import authentication.application.AuthUserFinder;
import authentication.application.dto.user.AuthUserResponse;
import backoffice.application.dto.membership.MembershipResponse;
import backoffice.application.membership.MembershipFinder;
import backoffice.domain.membership.MembershipId;
import booking.application.dto.membership_acquisition.MembershipAcquisitionError;
import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipAcquisitionCreator {
    AuthUserFinder authUserFinder = mock(AuthUserFinder.class);
    MembershipAcquisitionRepository membershipAcquisitionRepo = mock(MembershipAcquisitionRepository.class);
    MembershipFinder membershipFinder = mock(MembershipFinder.class);

    MembershipAcquisitionCreator creator = new MembershipAcquisitionCreator(authUserFinder, membershipAcquisitionRepo
            , membershipFinder);

    AuthUserResponse authUserExample = AuthUserResponse.of(
            "1",
            "john@doe.com",
            "",
            "",
            "",
            "",
            "",
            ""
    );

    @Test
    void itShouldReturnNoAuthUserWhenThereIsNoAuthenticatedUser() {
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.none());

        Either<MembershipAcquisitionError, Void> response = creator.create(new MembershipAcquisitionId(), "1");

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.NO_AUTHENTICATED_USER);
    }

    @Test
    void itShouldReturnMembershipNotFoundWhenThereIsNoMembershipWithIdProvided() {
        var membershipId = new MembershipId();
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserExample));
        when(membershipFinder.findById(membershipId)).thenReturn(Option.none());

        Either<MembershipAcquisitionError, Void> response = creator.create(
                new MembershipAcquisitionId(),
                membershipId.toString()
        );

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipAcquisitionError.MEMBERSHIP_NOT_FOUND);
    }

    @Test
    void itShouldStoreNewMembershipAcquisition() {
        var membershipId = new MembershipId();
        when(authUserFinder.findAuthenticatedUser()).thenReturn(Option.of(authUserExample));
        var membership = MembershipResponse.of(
                membershipId.toString(),
                "AWESOME",
                "desc",
                ImmutableSet.of("THURSDAY", "TUESDAY"),
                1000
        );
        when(membershipFinder.findById(membershipId)).thenReturn(Option.of(membership));
        when(membershipAcquisitionRepo.store(any())).thenReturn(Try.success(null));
        var id = new MembershipAcquisitionId();
        Either<MembershipAcquisitionError, Void> response = creator.create(id, membershipId.toString());

        assertThat(response.isRight()).isTrue();
        var expectedMembershipAcquisition = MembershipAcquisition.create(
                id,
                membershipId.toString(),
                "john@doe.com",
                1000,
                LocalDateTime.now(Clock.systemUTC()).getMonth(),
                ImmutableSet.of(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY)
        );
        verify(membershipAcquisitionRepo, times(1)).store(expectedMembershipAcquisition);
    }
}
