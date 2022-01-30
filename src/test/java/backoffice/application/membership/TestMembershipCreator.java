package backoffice.application.membership;

import backoffice.application.dto.membership.MembershipInformation;
import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.Sets;
import io.vavr.control.Either;
import io.vavr.control.Try;
import shared.application.UseCaseError;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestMembershipCreator {
    MembershipRepository membershipRepo = mock(MembershipRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    MembershipCreator membershipCreator = new MembershipCreator(membershipRepo, officeBranchFinder);

    @Test
    void itShouldReturnOfficeBranchNotFoundWhenOfficeBranchIdNotExist() {
        var officeBranchId = new OfficeBranchId();
        var membershipId = new MembershipId();
        MembershipInformation info = MembershipInformation.of(
                "Awesome membership",
                "Some desc",
                1000,
                Sets.newHashSet("MONDAY")
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.MEMBERSHIP)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));

        Either<UseCaseError, Void> response = membershipCreator.create(officeBranchId, membershipId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnOfficeBranchForbiddenWhenUseDoesNotHaveAccess() {
        var officeBranchId = new OfficeBranchId();
        var membershipId = new MembershipId();
        MembershipInformation info = MembershipInformation.of(
                "Awesome membership",
                "Some desc",
                1000,
                Sets.newHashSet("MONDAY")
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.MEMBERSHIP)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<UseCaseError, Void> response = membershipCreator.create(officeBranchId, membershipId, info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldStoreMembership() {
        var officeBranch = new OfficeBranchBuilder().build();
        var membershipId = new MembershipId();
        MembershipInformation info = MembershipInformation.of(
                "Awesome membership",
                "Some desc",
                1000,
                Sets.newHashSet("MONDAY")
        );
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.MEMBERSHIP)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(membershipRepo.store(any())).thenReturn(Try.success(null));

        membershipCreator.create(officeBranch.id(), membershipId, info);

        var expectedMembership = Membership.createNew(
                membershipId,
                "Awesome membership",
                "Some desc",
                1000,
                officeBranch
        );
        expectedMembership.configAccessDays(Sets.newHashSet(DayOfWeek.MONDAY));
        verify(membershipRepo, times(1)).store(expectedMembership);
    }
}
