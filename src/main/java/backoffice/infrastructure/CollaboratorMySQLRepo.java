package backoffice.infrastructure;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.CollaboratorRepository;
import backoffice.domain.collaborator.Status;
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
public class CollaboratorMySQLRepo
        extends BackofficeJPARepo<Collaborator, CollaboratorId> implements CollaboratorRepository {
    @Override
    public Try<Void> store(Collaborator collaborator) {
        return save(collaborator);
    }

    @Override
    public Try<Void> update(Collaborator collaborator) { return merge(collaborator); }

    @Override
    public Option<Collaborator> findById(CollaboratorId id) {
        Consumer<Root<Collaborator>> join = table -> table.fetch("officeBranch");
        Function3<
                CriteriaQuery<Collaborator>,
                Root<Collaborator>,
                CriteriaBuilder,
                CriteriaQuery<Collaborator>
                > addConstraints = (query, table, builder) ->
                query.select(table).where(builder.equal(table.get("id"), id));

        return findOne(addConstraints, join, Collaborator.class);
    }

    public Option<Collaborator> findWithRoles(CollaboratorId id) {
        Consumer<Root<Collaborator>> join = table -> {
            table.fetch("officeBranch");
            table.fetch("roles");
        };
        Function3<
                CriteriaQuery<Collaborator>,
                Root<Collaborator>,
                CriteriaBuilder,
                CriteriaQuery<Collaborator>
                > addConstraints = (query, table, builder) ->
                query.select(table).where(builder.equal(table.get("id"), id));

        return findOne(addConstraints, join, Collaborator.class);
    }

    @Override
    public List<Collaborator> find(OfficeBranch officeBranch) {
        var entityManager = entityManagerFactory.createEntityManager();
        Consumer<Root<Collaborator>> join = table -> { };
        Function3<
                CriteriaQuery<Collaborator>,
                Root<Collaborator>,
                CriteriaBuilder,
                CriteriaQuery<Collaborator>
                > addConstraints = (query, table, builder) ->
                query.select(table).where(builder.equal(table.get("officeBranch"), officeBranch));

        List<Collaborator> collaborators = findAll(entityManager, addConstraints, join, Collaborator.class);
        entityManager.close();
        return collaborators;
    }

    @Override
    public List<Collaborator> find(String email) {
        var entityManager = entityManagerFactory.createEntityManager();
        Consumer<Root<Collaborator>> join = table -> table.fetch("officeBranch");
        Function3<
                CriteriaQuery<Collaborator>,
                Root<Collaborator>,
                CriteriaBuilder,
                CriteriaQuery<Collaborator>
                > addConstraints = (query, table, builder) -> {
            var equalToEmail = builder.equal(table.get("email"), email);
            var isActive = builder.equal(table.get("status"), Status.ACTIVE);
            return query.select(table).where(builder.and(equalToEmail, isActive));
        };
        var collaborators = findAll(entityManager, addConstraints, join, getEntityClass());
        entityManager.close();
        return collaborators;
    }

    @Override
    public boolean exists(String email, OfficeBranch officeBranch) {
        return find(email, officeBranch).isDefined();
    }

    @Override
    public Option<Collaborator> find(String email, OfficeBranch officeBranch) {
        Consumer<Root<Collaborator>> join = table -> table.fetch("roles", JoinType.LEFT);
        Function3<
                CriteriaQuery<Collaborator>,
                Root<Collaborator>,
                CriteriaBuilder,
                CriteriaQuery<Collaborator>
                > addConstraints = (query, table, builder) -> {
            var equalToEmail = builder.equal(table.get("email"), email);
            var equalToOfficeBranch = builder.equal(table.get("officeBranch"), officeBranch);
            return query.select(table)
                    .where(builder.and(equalToEmail, equalToOfficeBranch));
        };

        return findOne(addConstraints, join, Collaborator.class);
    }

    @Override
    public Class<Collaborator> getEntityClass() {
        return Collaborator.class;
    }
}
