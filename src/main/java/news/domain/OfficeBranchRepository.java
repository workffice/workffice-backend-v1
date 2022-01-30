package news.domain;

import io.vavr.control.Option;

public interface OfficeBranchRepository {
    void store(OfficeBranch officeBranch);

    void update(OfficeBranch officeBranch);

    Option<OfficeBranch> findById(String officeBranchId);
}
