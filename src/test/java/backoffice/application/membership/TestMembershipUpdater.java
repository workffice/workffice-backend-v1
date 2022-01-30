package backoffice.application.membership;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.membership.MembershipError;
import backoffice.application.dto.membership.MembershipInformation;
import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.MembershipBuilder;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipUpdater {
    MembershipRepository membershipRepo = mock(MembershipRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    MembershipUpdater updater = new MembershipUpdater(membershipRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenMembershipDoesNotExist() {
        var membershipId = new MembershipId();
        when(membershipRepo.findById(membershipId)).thenReturn(Option.none());
        var info = MembershipInformation.of("New name", "New desc", 100, null);

        Either<MembershipError, Void> response = updater.update(membershipId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipError.MEMBERSHIP_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenUserHasNoPermsToDeleteMembership() {
        var membership = new MembershipBuilder().build();
        when(membershipRepo.findById(membership.id())).thenReturn(Option.of(membership));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.MEMBERSHIP))
        )).thenReturn(false);
        var info = MembershipInformation.of("New name", "New desc", 100, null);

        Either<MembershipError, Void> response = updater.update(membership.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipError.MEMBERSHIP_FORBIDDEN);
    }

    @Test
    void itShouldUpdateMembershipWithInfoProvided() {
        var membership = new MembershipBuilder().build();
        membership.configAccessDays(ImmutableSet.of(DayOfWeek.THURSDAY));
        when(membershipRepo.findById(membership.id())).thenReturn(Option.of(membership));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.MEMBERSHIP))
        )).thenReturn(true);
        when(membershipRepo.update(any())).thenReturn(Try.success(null));
        var info = MembershipInformation.of("New name", "New desc", 100, null);

        Either<MembershipError, Void> response = updater.update(membership.id(), info);

        assertThat(response.isRight()).isTrue();
        var expectedMembership = Membership.createNew(
                membership.id(),
                "New name",
                "New desc",
                100,
                membership.officeBranch()
        );
        expectedMembership.configAccessDays(ImmutableSet.of(DayOfWeek.THURSDAY));
        verify(membershipRepo, times(1)).update(expectedMembership);
    }
}
