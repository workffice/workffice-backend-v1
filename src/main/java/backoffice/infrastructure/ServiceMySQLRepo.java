package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.service.Service;
import backoffice.domain.service.ServiceId;
import backoffice.domain.service.ServiceRepository;
import io.vavr.Function3;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceMySQLRepo
        extends BackofficeJPARepo<Service, ServiceId>
        implements ServiceRepository {
    @Override
    public Try<Void> store(Service service) {
        return save(service);
    }

    @Override
    public List<Service> findByOfficeBranch(OfficeBranch officeBranch) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<
                CriteriaQuery<Service>,
                Root<Service>,
                CriteriaBuilder,
                CriteriaQuery<Service>
                > addConstraints = (query, table, builder) -> query.where(builder.equal(table.get("officeBranch"),
                officeBranch));
        Consumer<Root<Service>> join = table -> {
        };
        List<Service> services = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return services;
    }


    @Override
    public Class<Service> getEntityClass() {
        return Service.class;
    }
}

