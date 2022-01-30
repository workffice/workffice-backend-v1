package booking.infrastructure.repositories;

import booking.domain.membership_acquisiton.MembershipAcquisition;
import booking.domain.membership_acquisiton.MembershipAcquisitionId;
import booking.domain.membership_acquisiton.MembershipAcquisitionRepository;
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
public class MembershipAcquisitionMySQLRepo
        extends BookingJPARepo<MembershipAcquisition, MembershipAcquisitionId>
        implements MembershipAcquisitionRepository {
    @Override
    public Try<Void> store(MembershipAcquisition membershipAcquisition) {
        return save(membershipAcquisition);
    }

    @Override
    public Try<Void> update(MembershipAcquisition membershipAcquisition) {
        return merge(membershipAcquisition);
    }

    @Override
    public Option<MembershipAcquisition> findById(MembershipAcquisitionId id) {
        Function3<CriteriaQuery<MembershipAcquisition>,
                Root<MembershipAcquisition>, CriteriaBuilder,
                CriteriaQuery<MembershipAcquisition>> addConstraints =
                (query, table, builder) -> query.where(builder.equal(table.get("id"), id)).distinct(true);

        Consumer<Root<MembershipAcquisition>> join = table -> {
            table.fetch("accessDays");
            table.fetch("paymentInformation", JoinType.LEFT);
        };
        return findOne(addConstraints, join, getEntityClass());
    }

    @Override
    public List<MembershipAcquisition> find(String buyerEmail) {
        var entityManager = entityManagerFactory.createEntityManager();
        Function3<CriteriaQuery<MembershipAcquisition>,
                Root<MembershipAcquisition>, CriteriaBuilder,
                CriteriaQuery<MembershipAcquisition>> addConstraints =
                (query, table, builder) -> query
                        .where(builder.equal(table.get("buyerEmail"), buyerEmail))
                        .distinct(true);

        Consumer<Root<MembershipAcquisition>> join = table -> {
            table.fetch("accessDays");
            table.fetch("paymentInformation", JoinType.LEFT);
        };
        var result = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return result;
    }

    @Override
    public Class<MembershipAcquisition> getEntityClass() {
        return MembershipAcquisition.class;
    }
}
