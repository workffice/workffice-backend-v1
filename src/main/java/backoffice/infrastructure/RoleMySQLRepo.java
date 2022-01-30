package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.domain.role.RoleRepository;
import io.vavr.Function3;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class RoleMySQLRepo extends BackofficeJPARepo<Role, RoleId> implements RoleRepository {

    private Set<Permission> findPermissionsStored(EntityManager entityManager, Role role) {
        Set<Resource> resources = role.permissions().stream().map(Permission::resource).collect(Collectors.toSet());
        Function3<
                CriteriaQuery<Permission>,
                Root<Permission>,
                CriteriaBuilder,
                CriteriaQuery<Permission>
                > addConstraints =
                (query, table, builder) -> query.select(table).where(table.get("resource").in(resources));
        Consumer<Root<Permission>> join = table -> { };
        return findAll(entityManager, addConstraints, join, Permission.class)
                .stream()
                .filter(perm -> role.permissions().contains(perm)).collect(Collectors.toSet());
    }

    public Try<Void> store(Role role) {
        Consumer<EntityManager> storeRole = entityManager -> {
            Set<Permission> permsAlreadyStored = findPermissionsStored(entityManager, role);
            permsAlreadyStored.addAll(role.permissions());
            var newRole = Role.create(role.id(), role.name(), permsAlreadyStored, role.officeBranch());
            if (!role.isActive())
                newRole.markAsDeleted();
            entityManager.persist(newRole);
        };
        return this.executeWrite(storeRole);
    }

    @Override
    public Try<Void> update(Role role) {
        Consumer<EntityManager> updateRole = entityManager -> {
            Set<Permission> permsAlreadyStored = findPermissionsStored(entityManager, role);
            permsAlreadyStored.addAll(role.permissions());
            var newRole = Role.create(role.id(), role.name(), permsAlreadyStored, role.officeBranch());
            if (!role.isActive())
                newRole.markAsDeleted();
            entityManager.merge(newRole);
        };
        return this.executeWrite(updateRole);
    }

    @Override
    public List<Role> findByOfficeBranch(OfficeBranch officeBranch) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Consumer<Root<Role>> join = table -> table.fetch("permissions");
        /* Use distinct in the query because when fetch join multiple permissions
        we receive multiple rows of the same role */
        Function3<
                CriteriaQuery<Role>,
                Root<Role>,
                CriteriaBuilder,
                CriteriaQuery<Role>
                > addConstraints = (query, table, builder) -> {
            var isRelatedWithOfficeBranch = builder.equal(table.get("officeBranch"), officeBranch);
            var isActive = builder.equal(table.get("deleted"), false);
            return query
                    .select(table)
                    .where(builder.and(isRelatedWithOfficeBranch, isActive)).distinct(true);
        };
        List<Role> roles = findAll(entityManager, addConstraints, join, Role.class);
        entityManager.close();
        return roles;
    }

    @Override
    public Option<Role> findById(RoleId id) {
        Function3<
                CriteriaQuery<Role>,
                Root<Role>,
                CriteriaBuilder,
                CriteriaQuery<Role>
                > addConstraints =
                (query, table, builder) -> query.select(table).where(builder.equal(table.get("id"), id));
        Consumer<Root<Role>> join = table -> table.fetch("officeBranch");
        return findOne(addConstraints, join, getEntityClass());
    }

    @Override
    public Class<Role> getEntityClass() {
        return Role.class;
    }
}
