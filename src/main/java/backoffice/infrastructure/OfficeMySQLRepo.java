package backoffice.infrastructure;

import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.OfficeRepository;
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
public class OfficeMySQLRepo
        extends BackofficeJPARepo<Office, OfficeId> implements OfficeRepository {

    @Override
    public Try<Void> store(Office office) {
        return save(office);
    }

    @Override
    public Try<Void> update(Office office) { return merge(office); }

    public Option<Office> findById(OfficeId id) {
        Function3<
                CriteriaQuery<Office>,
                Root<Office>,
                CriteriaBuilder,
                CriteriaQuery<Office>
                > addConstraints = (query, table, builder) -> query
                .where(builder.equal(table.get("id"), id)).distinct(true);
        Consumer<Root<Office>> join = table -> {
            table.fetch("officeBranch");
            table.fetch("services", JoinType.LEFT);
            table.fetch("equipments", JoinType.LEFT);
        };
        return findOne(addConstraints, join, getEntityClass());
    }

    @Override
    public List<Office> findByOfficeBranch(OfficeBranch officeBranch) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<
                CriteriaQuery<Office>,
                Root<Office>,
                CriteriaBuilder,
                CriteriaQuery<Office>
                > addConstraints = (query, table, builder) -> query
                .where(builder.equal(table.get("officeBranch"), officeBranch)).distinct(true);
        Consumer<Root<Office>> join = table -> {
            table.fetch("services", JoinType.LEFT);
            table.fetch("equipments", JoinType.LEFT);
        };
        List<Office> offices = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return offices;
    }

    @Override
    public Class<Office> getEntityClass() {
        return Office.class;
    }
}
