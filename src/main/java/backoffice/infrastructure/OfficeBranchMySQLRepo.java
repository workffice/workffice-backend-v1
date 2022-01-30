package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolder;
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
public class OfficeBranchMySQLRepo
        extends BackofficeJPARepo<OfficeBranch, OfficeBranchId> implements OfficeBranchRepository {

    @Override
    public Try<Void> store(OfficeBranch officeBranch) {
        return save(officeBranch);
    }

    @Override
    public Try<Void> update(OfficeBranch officeBranch) {
        return merge(officeBranch);
    }

    @Override
    public Option<OfficeBranch> findById(OfficeBranchId id) {
        Function3<
                CriteriaQuery<OfficeBranch>,
                Root<OfficeBranch>,
                CriteriaBuilder,
                CriteriaQuery<OfficeBranch>
                > constraints = (query, table, criteriaBuilder) -> {
            var hasId = criteriaBuilder.equal(table.get("id"), id);
            var isNotDeleted = criteriaBuilder.equal(table.get("deleted"), false);
            return query.where(criteriaBuilder.and(hasId, isNotDeleted));
        };
        Consumer<Root<OfficeBranch>> joins = table -> {
            table.fetch("images", JoinType.LEFT);
            table.fetch("location");
        };
        return findOne(constraints, joins, getEntityClass());
    }

    @Override
    public List<OfficeBranch> findByOfficeHolder(OfficeHolder officeHolder) {
        var entityManager = entityManagerFactory.createEntityManager();
        /* Use distinct in the query because when fetch join multiple images
        we receive multiple rows of the same office branch */
        Function3<
                CriteriaQuery<OfficeBranch>,
                Root<OfficeBranch>,
                CriteriaBuilder,
                CriteriaQuery<OfficeBranch>
                > constraints = (query, table, criteriaBuilder) -> {
            var hasOwner = criteriaBuilder.equal(table.get("owner"), officeHolder);
            var isNotDeleted = criteriaBuilder.equal(table.get("deleted"), false);
            return query.where(criteriaBuilder.and(hasOwner, isNotDeleted)).distinct(true);
        };
        Consumer<Root<OfficeBranch>> joins = table -> {
            table.fetch("images", JoinType.LEFT);
            table.fetch("location");
        };
        var officeBranches = findAll(entityManager, constraints, joins, getEntityClass());
        entityManager.close();
        return officeBranches;
    }


    public List<OfficeBranch> findByIds(List<OfficeBranchId> ids) {
        var entityManager = entityManagerFactory.createEntityManager();
        /* Use distinct in the query because when fetch join multiple images
        we receive multiple rows of the same office branch */
        Function3<
                CriteriaQuery<OfficeBranch>,
                Root<OfficeBranch>,
                CriteriaBuilder,
                CriteriaQuery<OfficeBranch>
                > constraints = (query, table, criteriaBuilder) -> {
            var hasId = table.get("id").in(ids);
            var isNotDeleted = criteriaBuilder.equal(table.get("deleted"), false);
            return query.where(criteriaBuilder.and(hasId, isNotDeleted)).distinct(true);
        };
        Consumer<Root<OfficeBranch>> joins = table -> {
            table.fetch("images", JoinType.LEFT);
            table.fetch("location");
        };
        var officeBranches = findAll(entityManager, constraints, joins, getEntityClass());
        entityManager.close();
        return officeBranches;
    }

    @Override
    public Class<OfficeBranch> getEntityClass() {
        return OfficeBranch.class;
    }
}
