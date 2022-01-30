package backoffice.application.membership;

import backoffice.application.dto.membership.MembershipResponse;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import io.vavr.control.Option;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MembershipFinder {
    private final OfficeBranchFinder   officeBranchFinder;
    private final MembershipRepository membershipRepo;

    public MembershipFinder(MembershipRepository membershipRepo, OfficeBranchFinder officeBranchFinder) {
        this.membershipRepo     = membershipRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    public Option<List<MembershipResponse>> findByOfficeBranch(OfficeBranchId officeBranchId) {
        return officeBranchFinder.find(officeBranchId)
                .map(officeBranch -> membershipRepo.find(OfficeBranch.fromDTO(officeBranch)))
                .map(memberships -> memberships.stream()
                        .filter(membership -> !membership.isDeleted())
                        .map(Membership::toResponse)
                        .collect(Collectors.toList()));
    }

    public Option<MembershipResponse> findById(MembershipId id) {
        return membershipRepo
                .findById(id)
                .map(Membership::toResponse);
    }
}
