package backoffice.domain.membership;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface MembershipRepository {
    
    Try<Void> store(Membership membership);

    Try<Void> update(Membership membership);
    
    Option<Membership> findById(MembershipId id);

    List<Membership> find(OfficeBranch officeBranch);
}
