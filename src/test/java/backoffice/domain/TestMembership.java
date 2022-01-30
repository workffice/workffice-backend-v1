package backoffice.domain;

import backoffice.application.dto.membership.MembershipInformation;
import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableSet;

import java.time.DayOfWeek;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMembership {

    @Test
    void itShouldUpdateMembershipWithInfoSpecified() {
        var officeBranch = new OfficeBranchBuilder().build();
        var membershipId = new MembershipId();
        var membership = Membership.createNew(
                membershipId,
                "AWESOME MEMBERSHIP",
                "Some desc",
                12,
                officeBranch
        );

        var info = MembershipInformation.of(
                "New name",
                "New desc",
                100,
                ImmutableSet.of("MONDAY")
        );
        var membershipUpdated = membership.update(info);

        var expectedMembership = Membership.createNew(
                membershipId,
                "New name",
                "New desc",
                100,
                officeBranch
        );
        expectedMembership.configAccessDays(ImmutableSet.of(DayOfWeek.MONDAY));
        assertThat(membershipUpdated).isEqualTo(expectedMembership);
    }

    @Test
    void itShouldNotUpdateMembershipWhenThereAreNullValues() {
        var officeBranch = new OfficeBranchBuilder().build();
        var membershipId = new MembershipId();
        var membership = Membership.createNew(
                membershipId,
                "AWESOME MEMBERSHIP",
                "Some desc",
                12,
                officeBranch
        );
        membership.configAccessDays(ImmutableSet.of(DayOfWeek.MONDAY));

        var info = MembershipInformation.of(
                null,
                "New desc",
                null,
                null
        );
        var membershipUpdated = membership.update(info);

        var expectedMembership = Membership.createNew(
                membershipId,
                "AWESOME MEMBERSHIP",
                "New desc",
                12,
                officeBranch
        );
        expectedMembership.configAccessDays(ImmutableSet.of(DayOfWeek.MONDAY));
        assertThat(membershipUpdated).isEqualTo(expectedMembership);
    }

    @Test
    void itShouldNotUpdateAccessDaysWhenTheyAreEmpty() {
        var officeBranch = new OfficeBranchBuilder().build();
        var membershipId = new MembershipId();
        var membership = Membership.createNew(
                membershipId,
                "AWESOME MEMBERSHIP",
                "Some desc",
                12,
                officeBranch
        );
        membership.configAccessDays(ImmutableSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        var info = MembershipInformation.of(
                "New name",
                "New desc",
                150,
                Collections.emptySet()
        );
        var membershipUpdated = membership.update(info);

        var expectedMembership = Membership.createNew(
                membershipId,
                "New name",
                "New desc",
                150,
                officeBranch
        );
        expectedMembership.configAccessDays(ImmutableSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        assertThat(membershipUpdated).isEqualTo(expectedMembership);
    }
}
