package backoffice.application.membership;

import backoffice.application.dto.membership.MembershipError;
import backoffice.application.dto.membership.MembershipInformation;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import shared.application.UseCaseError;

import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MembershipCreator {
    
    private final OfficeBranchFinder   officeBranchFinder;
    private final MembershipRepository membershipRepo;

    public MembershipCreator(MembershipRepository membershipRepo, OfficeBranchFinder officeBranchFinder) {
        this.membershipRepo     = membershipRepo;
        this.officeBranchFinder = officeBranchFinder;
    }
    
    private Membership createMembership(
            MembershipId id,
            MembershipInformation info,
            OfficeBranch officeBranch
    ) {
        Set<DayOfWeek> accessDays = info.getAccessDays()
                .stream()
                .map(DayOfWeek::valueOf).collect(Collectors.toSet());
        var membership = Membership.createNew(
                id,
                info.getName(),
                info.getDescription(),
                info.getPricePerMonth(),
                officeBranch
        );
        membership.configAccessDays(accessDays);
        return membership;
    }
    
    public Either<UseCaseError, Void> create(
            OfficeBranchId officeBranchId,
            MembershipId membershipId,
            MembershipInformation info
    ) {
        return officeBranchFinder
                .findWithAuthorization(officeBranchId, Permission.create(Access.WRITE, Resource.MEMBERSHIP))
                .map(OfficeBranch::fromDTO)
                .map(officeBranch -> createMembership(membershipId, info, officeBranch))
                .flatMap(membership -> membershipRepo.store(membership).toEither(MembershipError.DB_ERROR));
    }
}
