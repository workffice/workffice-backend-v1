package backoffice.domain;

import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import backoffice.factories.OfficeBranchBuilder;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRole {
    
    @Test
    void itShouldReturnFalseWhenItDoesNotHaveAnyPermissionWithResourceAndAccessLevel() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permissions = Set.of(Permission.create(Access.READ, Resource.OFFICE));
        Role role = Role.create(new RoleId(), "Office role", permissions, officeBranch);
        
        assertThat(role.hasAccessTo(Permission.create(Access.WRITE, Resource.ROLE))).isFalse();
    }
    
    @Test
    void itShouldReturnTrueWhenItHaveAtLeastAPermissionWithResourceAndAccessLevel() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permissions = Set.of(
                Permission.create(Access.READ, Resource.OFFICE),
                Permission.create(Access.WRITE, Resource.OFFICE)
        );
        Role role = Role.create(new RoleId(), "Office role", permissions, officeBranch);
        
        assertThat(role.hasAccessTo(Permission.create(Access.WRITE, Resource.OFFICE))).isTrue();
    }

    @Test
    void itShouldReturnTrueWhenRoleHasWriteAccessToResourceAndAskWithTheSameResourceForReadAccess() {
        var officeBranch = new OfficeBranchBuilder().build();
        var permissions = Set.of(Permission.create(Access.WRITE, Resource.OFFICE));
        Role role = Role.create(new RoleId(), "Office role", permissions, officeBranch);

        var readPermission = Permission.create(Access.READ, Resource.OFFICE);
        assertThat(role.hasAccessTo(readPermission)).isTrue();
    }
}
