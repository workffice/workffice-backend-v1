package shared.infrastructure;

import io.vavr.CheckedRunnable;
import io.vavr.Function3;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public abstract class JPARepository<E, EID> {
    protected abstract EntityManagerFactory getEntityManagerFactory();

    protected Try<Void> executeWrite(Consumer<EntityManager> writeQuery) {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        CheckedRunnable persist = () -> {
            transaction.begin();
            writeQuery.accept(entityManager);
            transaction.commit();
        };
        return Try.run(persist)
                .onFailure((e) -> transaction.rollback())
                .andFinally(entityManager::close);
    }

    protected Try<Void> merge(E entity) {
        Consumer<EntityManager> update = entityManager -> entityManager.merge(entity);
        return this.executeWrite(update);
    }

    protected Try<Void> save(E entity) {
        Consumer<EntityManager> insert = entityManager -> entityManager.persist(entity);
        return this.executeWrite(insert);
    }

    protected <T> Option<T> findOne(
            Function3<CriteriaQuery<T>, Root<T>, CriteriaBuilder, CriteriaQuery<T>> addConstraints,
            Consumer<Root<T>> join,
            Class<T> classType
    ) {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(classType);
        Root<T> table = query.from(classType);
        join.accept(table);
        CriteriaQuery<T> queryWithConstraints = addConstraints.apply(query, table, builder);
        Optional<T> result = entityManager
                .createQuery(queryWithConstraints)
                .getResultList()
                .stream()
                .findFirst();
        entityManager.close();
        return Option.ofOptional(result);
    }

    protected <T> List<T> findAll(
            EntityManager entityManager,
            Function3<CriteriaQuery<T>, Root<T>, CriteriaBuilder, CriteriaQuery<T>> addConstraints,
            Consumer<Root<T>> join,
            Class<T> classType
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(classType);
        Root<T> table = query.from(classType);
        join.accept(table);
        CriteriaQuery<T> queryWithConstraints = addConstraints.apply(query, table, builder);
        return entityManager
                .createQuery(queryWithConstraints)
                .getResultList();
    }

    protected <T> List<T> findAll(
            EntityManager entityManager,
            Function3<CriteriaQuery<T>, Root<T>, CriteriaBuilder, CriteriaQuery<T>> addConstraints,
            Consumer<Root<T>> join,
            Integer offset,
            Integer limit,
            Class<T> classType
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(classType);
        Root<T> table = query.from(classType);
        join.accept(table);
        CriteriaQuery<T> queryWithConstraints = addConstraints.apply(query, table, builder);
        return entityManager
                .createQuery(queryWithConstraints)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    protected <T> Option<E> findByColumn(String column, T value) {
        Function3<
                CriteriaQuery<E>,
                Root<E>,
                CriteriaBuilder,
                CriteriaQuery<E>
                > addConstraints =
                (query, table, builder) -> query.select(table).where(builder.equal(table.get(column), value));
        Consumer<Root<E>> join = table -> { };
        return findOne(addConstraints, join, getEntityClass());
    }

    protected Option<E> findById(EID id) {
        return findByColumn("id", id);
    }

    abstract public Class<E> getEntityClass();
}
