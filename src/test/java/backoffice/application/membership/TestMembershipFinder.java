package backoffice.application.membership;

import backoffice.application.dto.membership.MembershipResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.factories.MembershipBuilder;
import backoffice.factories.OfficeBranchBuilder;
import com.google.inject.internal.util.ImmutableList;
import io.vavr.control.Option;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMembershipFinder {
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    MembershipRepository membershipRepo = mock(MembershipRepository.class);

    MembershipFinder finder = new MembershipFinder(membershipRepo, officeBranchFinder);

    @Test
    void itShouldReturnNoneWhenOfficeBranchNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.find(officeBranchId)).thenReturn(Option.none());

        Option<List<MembershipResponse>> result = finder.findByOfficeBranch(officeBranchId);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnMembershipsRelatedWithOfficeBranch() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        var membership1 = new MembershipBuilder().build();
        var membership2 = new MembershipBuilder().build();
        var membership3 = new MembershipBuilder().build();
        when(membershipRepo.find(any(OfficeBranch.class))).thenReturn(ImmutableList.of(
                membership1,
                membership2,
                membership3
        ));

        Option<List<MembershipResponse>> result = finder.findByOfficeBranch(officeBranch.id());

        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).containsExactlyInAnyOrder(
            membership1.toResponse(),
            membership2.toResponse(),
            membership3.toResponse()
        );
    }

    @Test
    void itShouldReturnFilterDeletedMemberships() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.find(officeBranch.id())).thenReturn(Option.of(officeBranch.toResponse()));
        var membership1 = new MembershipBuilder().build();
        var membership2 = new MembershipBuilder().build();
        membership2.delete();
        var membership3 = new MembershipBuilder().build();
        when(membershipRepo.find(any(OfficeBranch.class))).thenReturn(ImmutableList.of(
                membership1,
                membership2,
                membership3
        ));

        Option<List<MembershipResponse>> result = finder.findByOfficeBranch(officeBranch.id());

        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).containsExactlyInAnyOrder(
                membership1.toResponse(),
                membership3.toResponse()
        );
    }
}
