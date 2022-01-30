package backoffice.infrastructure;

import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.membership.MembershipRepository;
import backoffice.domain.office_branch.OfficeBranch;
import io.vavr.Function3;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipMySQLRepo
        extends BackofficeJPARepo<Membership, MembershipId> implements MembershipRepository {

    @Override
    public Try<Void> store(Membership membership) {
        return save(membership);
    }

    @Override
    public Try<Void> update(Membership membership) {
        return merge(membership);
    }

    @Override
    public Option<Membership> findById(MembershipId id) {
        Function3<CriteriaQuery<Membership>, Root<Membership>,
                CriteriaBuilder, CriteriaQuery<Membership>> addConstraints =
                (query, table, builder) -> query
                        .where(builder.equal(table.get("id"), id))
                        .distinct(true);
        Consumer<Root<Membership>> join = table -> {
            table.fetch("officeBranch");
            table.fetch("accessDays", JoinType.LEFT);
        };
        return findOne(addConstraints, join, getEntityClass());
    }

    @Override
    public List<Membership> find(OfficeBranch officeBranch) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<CriteriaQuery<Membership>, Root<Membership>,
                CriteriaBuilder, CriteriaQuery<Membership>> addConstraints =
                (query, table, builder) -> query
                        .where(builder.equal(table.get("officeBranch"), officeBranch))
                        .distinct(true);
        Consumer<Root<Membership>> join = table -> table.fetch("accessDays", JoinType.LEFT);
        var result = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return result;
    }

    @Override
    public Class<Membership> getEntityClass() {
        return Membership.class;
    }
}
