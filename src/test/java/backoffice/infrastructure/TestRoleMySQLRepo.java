package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeHolderBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.Sets;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestRoleMySQLRepo {
    @Autowired
    RoleMySQLRepo roleMySQLRepo;
    @Autowired
    OfficeBranchMySQLRepo officeBranchMySQLRepo;
    @Autowired
    OfficeHolderMySQLRepo officeHolderMySQLRepo;
    
    OfficeBranch createOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        officeHolderMySQLRepo.store(officeHolder);
        officeBranchMySQLRepo.store(officeBranch);
        return officeBranch;
    }

    @Test
    void itShouldStoreRoleWithPermissionsSpecified() {
        var officeBranch = createOfficeBranch();
        var permissions = Set.of(Permission.create(Access.READ, Resource.OFFICE));
        RoleId id = new RoleId();
        Role role = Role.create(id, "Super role", permissions, officeBranch);
        
        var response = roleMySQLRepo.store(role);
        
        assertThat(response.isFailure()).isFalse();
        Role roleStored = roleMySQLRepo.findById(id).get();
        assertThat(roleStored.id()).isEqualTo(id);
        assertThat(roleStored.name()).isEqualTo("Super role");
        assertThat(roleStored.permissions()).isEqualTo(permissions);
    }
    
    @Test
    void itShouldNotDuplicatePermissionsWhenStoredDifferentRolesWithSamePermissions() {
        var officeBranch = createOfficeBranch();
        var permissions = Set.of(
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.WRITE, Resource.OFFICE)
        );
        var permissions2 = Set.of(
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.WRITE, Resource.OFFICE)
        );
        RoleId id = new RoleId();
        RoleId id2 = new RoleId();
        Role role = Role.create(id, "Super role", permissions, officeBranch);
        Role role2 = Role.create(id2, "Another role", permissions2, officeBranch);
    
        roleMySQLRepo.store(role).get();
        roleMySQLRepo.store(role2).get();
        
        Role roleStored = roleMySQLRepo.findById(id).get();
        Role roleStored2 = roleMySQLRepo.findById(id2).get();
        Set<Long> permIdsFromRole1 = roleStored.permissions().stream().map(Permission::id).collect(Collectors.toSet());
        Set<Long> permIdsFromRole2 = roleStored2.permissions().stream().map(Permission::id).collect(Collectors.toSet());
        assertThat(permIdsFromRole1).isEqualTo(permIdsFromRole2);
    }
    
    @Test
    void itShouldStorePermissionsSpecifiedForEachRole() {
        var officeBranch = createOfficeBranch();
        var permissions = Set.of(
                Permission.create(Access.READ, Resource.ROLE),
                Permission.create(Access.WRITE, Resource.OFFICE)
        );
        var permissions2 = Set.of(
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.WRITE, Resource.ROLE)
        );
        RoleId id = new RoleId();
        RoleId id2 = new RoleId();
        Role role = Role.create(id, "Super role", permissions, officeBranch);
        Role role2 = Role.create(id2, "Another role", permissions2, officeBranch);
    
        roleMySQLRepo.store(role).get();
        roleMySQLRepo.store(role2).get();
    
        Role roleStored = roleMySQLRepo.findById(id).get();
        Role roleStored2 = roleMySQLRepo.findById(id2).get();
        assertThat(roleStored.permissions()).isEqualTo(permissions);
        assertThat(roleStored2.permissions()).isEqualTo(permissions2);
    }
    
    @Test
    void itShouldReturnAllActiveRolesRelatedWithOfficeBranch() {
        var officeBranch = createOfficeBranch();
        var permissions = Set.of(Permission.create(Access.READ, Resource.OFFICE));
        Role role = Role.create(new RoleId(), "Super role", permissions, officeBranch);
        Role role2 = Role.create(new RoleId(), "Another role", permissions, officeBranch);
        Role role3 = Role.create(new RoleId(), "Some role", permissions, officeBranch);
        role3.markAsDeleted();
        roleMySQLRepo.store(role);
        roleMySQLRepo.store(role2);
        roleMySQLRepo.store(role3);

        var roles = roleMySQLRepo.findByOfficeBranch(officeBranch);
        
        assertThat(roles).containsExactlyInAnyOrder(role, role2);
        assertThat(roles.get(0).permissions()).size().isEqualTo(1);
        assertThat(roles.get(1).permissions()).size().isEqualTo(1);
    }

    @Test
    void itShouldReturnRoleWithRoleIdSpecified() {
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleMySQLRepo.store(role);

        Option<Role> roleStored = roleMySQLRepo.findById(role.id());

        assertThat(roleStored.isDefined()).isTrue();
        assertThat(roleStored.get().officeBranch().id()).isEqualTo(officeBranch.id());
    }

    @Test
    void itShouldReturnEmptyWhenThereIsNoRoleWithIdSpecified() {
        Option<Role> roleStored = roleMySQLRepo.findById(new RoleId());

        assertThat(roleStored.isEmpty()).isTrue();
    }

    @Test
    void itShouldUpdateRole() {
        var officeBranch = createOfficeBranch();
        var role = new RoleBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        roleMySQLRepo.store(role);

        var roleWithUpdatedProps = Role.create(
                role.id(),
                "New awesome name",
                Sets.newHashSet(Permission.create(Access.READ, Resource.ROLE)),
                role.officeBranch()
        );
        roleMySQLRepo.update(roleWithUpdatedProps);

        var roleUpdated = roleMySQLRepo.findById(role.id()).get();
        assertThat(roleUpdated.name()).isEqualTo("New awesome name");
        assertThat(roleUpdated.permissions()).containsExactly(Permission.create(Access.READ, Resource.ROLE));
    }
}
