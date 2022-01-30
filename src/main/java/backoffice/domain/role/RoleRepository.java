package backoffice.domain.role;

import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface RoleRepository {

    Try<Void> store(Role role);

    Try<Void> update(Role role);

    Option<Role> findById(RoleId id);

    List<Role> findByOfficeBranch(OfficeBranch officeBranch);
}
