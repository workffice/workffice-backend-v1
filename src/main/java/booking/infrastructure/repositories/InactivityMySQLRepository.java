package booking.infrastructure.repositories;

import booking.domain.inactivity.Inactivity;
import booking.domain.inactivity.InactivityId;
import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.persistence.RollbackException;
import org.springframework.stereotype.Repository;

@Repository
public class InactivityMySQLRepository extends BookingJPARepo<Inactivity, InactivityId> {

    public Try<Void> store(Inactivity inactivity) {
        return save(inactivity);
    }

    public Option<Inactivity> findById(InactivityId id) {
        return super.findById(id);
    }

    public Try<Void> delete(InactivityId id) {
        var entityManager = entityManagerFactory.createEntityManager();
        var transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            var inactivity = entityManager.find(getEntityClass(), id);
            if (inactivity == null)
                return Try.success(null);
            entityManager.remove(inactivity);
            transaction.commit();
            return Try.success(null);
        } catch (IllegalArgumentException | RollbackException e) {
            transaction.rollback();
            return Try.failure(e);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public Class<Inactivity> getEntityClass() {
        return Inactivity.class;
    }
}
