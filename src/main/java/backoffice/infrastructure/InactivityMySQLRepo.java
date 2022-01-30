package backoffice.infrastructure;

import backoffice.domain.office.Office;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityRepository;
import io.vavr.Function3;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class InactivityMySQLRepo
        extends BackofficeJPARepo<Inactivity, InactivityId> implements InactivityRepository {

    @Override
    public Try<Void> store(Inactivity inactivity) {
        return save(inactivity);
    }

    @Override
    public Try<Void> bulkStore(List<Inactivity> inactivities) {
        Consumer<EntityManager> bulkSave = entityManager -> {
            for (Inactivity inactivity : inactivities) {
                entityManager.persist(inactivity);
            }
        };
        return super.executeWrite(bulkSave);
    }

    @Override
    public List<Inactivity> findAllByOffice(Office office) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<
                CriteriaQuery<Inactivity>,
                Root<Inactivity>,
                CriteriaBuilder,
                CriteriaQuery<Inactivity>
                > addConstraints =
                (query, table, builder)-> query.where(builder.equal(table.get("office"), office));
        Consumer<Root<Inactivity>> join = table -> { };
        List<Inactivity> inactivities = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return inactivities;
    }

    @Override
    public Try<Void> delete(List<Inactivity> inactivities) {
        Consumer<EntityManager> deleteQuery = entityManager -> {
            for (Inactivity inactivity : inactivities) {
                entityManager.remove(inactivity);
            }
        };
        return super.executeWrite(deleteQuery);
    }

    @Override
    public Class<Inactivity> getEntityClass() {
        return Inactivity.class;
    }
}
