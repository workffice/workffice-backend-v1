package booking.infrastructure.repositories;

import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.OfficeRepository;
import io.vavr.Function3;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.function.Consumer;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class OfficeMySQLRepository
        extends BookingJPARepo<Office, OfficeId> implements OfficeRepository {
    @Override
    public Try<Void> store(Office office) {
        return save(office);
    }

    @Override
    public Try<Void> update(Office office) {
        return merge(office);
    }

    @Override
    public Option<Office> findById(OfficeId id) {
        Function3<
                CriteriaQuery<Office>,
                Root<Office>,
                CriteriaBuilder,
                CriteriaQuery<Office>
                > addConstraints =
                (query, table, builder) -> query.where(builder.equal(table.get("id"), id));
        Consumer<Root<Office>> join = table -> table.fetch("inactivities", JoinType.LEFT);
        return findOne(addConstraints, join, getEntityClass());
    }

    @Override
    public Class<Office> getEntityClass() {
        return Office.class;
    }
}
