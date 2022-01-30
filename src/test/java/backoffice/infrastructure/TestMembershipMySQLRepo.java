package backoffice.infrastructure;

import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.Sets;
import server.WorkfficeApplication;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestMembershipMySQLRepo {
    @Autowired
    MembershipMySQLRepo membershipRepo;
    @Autowired
    OfficeHolderRepository officeHolderRepo;
    @Autowired
    OfficeBranchRepository officeBranchRepo;

    private OfficeBranch createOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        officeHolderRepo.store(officeBranch.owner());
        officeBranchRepo.store(officeBranch);
        return officeBranch;
    }

    @Test
    void itShouldStoreMembershipWithDaysOfWeekSettled() {
        var officeBranch = createOfficeBranch();
        var membershipId = new MembershipId();
        var membership = Membership.createNew(
                membershipId,
                "Awesome membership",
                "Some desc",
                1000,
                officeBranch
        );
        membership.configAccessDays(Sets.newHashSet(DayOfWeek.MONDAY));

        membershipRepo.store(membership);
        var membershipStored = membershipRepo.findById(membershipId).get();

        assertThat(membershipStored).isEqualTo(membership);
    }

    @Test
    void itShouldReturnMembershipsRelatedWithOfficeBranch() {
        var officeBranch = createOfficeBranch();
        var membership = Membership.createNew(
                new MembershipId(),
                "Awesome membership",
                "Some desc",
                1000,
                officeBranch
        );
        membership.configAccessDays(Sets.newHashSet(DayOfWeek.MONDAY));
        membershipRepo.store(membership);
        var membership2 = Membership.createNew(
                new MembershipId(),
                "Awesome membership",
                "Some desc",
                1000,
                officeBranch
        );
        membership2.configAccessDays(Sets.newHashSet(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        membershipRepo.store(membership2);

        var memberships = membershipRepo.find(officeBranch);

        assertThat(memberships).size().isEqualTo(2);
    }

    @Test
    void itShouldUpdateMembership() {
        var officeBranch = createOfficeBranch();
        var membershipId = new MembershipId();
        var membership = Membership.createNew(
                membershipId,
                "Awesome membership",
                "Some desc",
                1000,
                officeBranch
        );
        membership.configAccessDays(Sets.newHashSet(DayOfWeek.MONDAY));
        membershipRepo.store(membership);

        membership.delete();
        membershipRepo.update(membership);

        var membershipStored = membershipRepo.findById(membershipId).get();

        assertThat(membershipStored.isDeleted()).isTrue();
    }
}
