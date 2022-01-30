package backoffice.factories;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.domain.role.Role;
import backoffice.domain.role.RoleId;
import com.github.javafaker.Faker;
import com.google.common.collect.Sets;

import java.util.Set;

public class RoleBuilder {
    private final Faker faker = new Faker();
    private String name = faker.name().name();
    private Set<Permission> permissions = Sets.newHashSet(
            Permission.create(Access.WRITE, Resource.OFFICE)
    );
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();
    
    public RoleBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public RoleBuilder withPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
        return this;
    }
    
    public RoleBuilder withOfficeBranch(OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }
    
    public Role build() {
        return Role.create(new RoleId(), name, permissions, officeBranch);
    }
}
