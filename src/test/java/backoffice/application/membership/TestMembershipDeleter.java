package backoffice.application.membership;

import backoffice.application.PermissionValidator;
import backoffice.application.dto.membership.MembershipError;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.MembershipBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipDeleter {
    MembershipRepository membershipRepo = mock(MembershipRepository.class);
    PermissionValidator permissionValidator = mock(PermissionValidator.class);

    MembershipDeleter deleter = new MembershipDeleter(membershipRepo, permissionValidator);

    @Test
    void itShouldReturnNotFoundWhenMembershipNotExist() {
        var membershipId = new MembershipId();
        when(membershipRepo.findById(membershipId)).thenReturn(Option.none());

        Either<MembershipError, Void> response = deleter.delete(membershipId);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipError.MEMBERSHIP_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToDeleteMembership() {
        var membership = new MembershipBuilder().build();
        when(membershipRepo.findById(membership.id())).thenReturn(Option.of(membership));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.MEMBERSHIP))
        )).thenReturn(false);

        Either<MembershipError, Void> response = deleter.delete(membership.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(MembershipError.MEMBERSHIP_FORBIDDEN);
    }

    @Test
    void itShouldDeleteMembership() {
        var membership = new MembershipBuilder().build();
        when(membershipRepo.findById(membership.id())).thenReturn(Option.of(membership));
        when(permissionValidator.userHasPerms(
                any(OfficeBranch.class),
                eq(Permission.create(Access.WRITE, Resource.MEMBERSHIP))
        )).thenReturn(true);
        when(membershipRepo.update(any())).thenReturn(Try.success(null));

        Either<MembershipError, Void> response = deleter.delete(membership.id());

        assertThat(response.isRight()).isTrue();
        membership.delete();
        verify(membershipRepo, times(1)).update(membership);
    }
}
