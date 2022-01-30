package search.domain;

import io.vavr.control.Option;
import search.domain.spec.Specification;

import java.util.List;

public interface OfficeBranchRepository {

    void store(OfficeBranch officeBranch);

    void update(OfficeBranch officeBranch);

    void delete(String officeBranchId);

    Option<OfficeBranch> findById(String id);

    List<OfficeBranch> search(Specification spec);

    List<OfficeBranch> search(List<Specification> spec, Integer offset, Integer limit);

    Long count(List<Specification> specs);
}
