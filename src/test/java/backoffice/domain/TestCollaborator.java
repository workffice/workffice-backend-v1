package backoffice.domain;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.CollaboratorBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.RoleBuilder;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCollaborator {

    @Test
    void itShouldNotBeActiveWhenCollaboratorIsCreated() {
        var collaborator = Collaborator.createNew(
                new CollaboratorId(),
                "bla@mail.com",
                "pirlonzio",
                Collections.emptySet(),
                new OfficeBranchBuilder().build()
        );

        assertThat(collaborator.isActive()).isFalse();
    }

    @Test
    void itShouldReturnTrueWhen2CollaboratorsHaveTheSameIdAndEmailAndStatus() {
        var collaboratorId = new CollaboratorId();
        var collaborator = Collaborator.createNew(
                collaboratorId,
                "bla@mail.com",
                "pirlonzio",
                Collections.emptySet(),
                new OfficeBranchBuilder().build()
        );
        var collaborator2 = Collaborator.createNew(
                collaboratorId,
                "bla@mail.com",
                "pirlonzio",
                Collections.emptySet(),
                new OfficeBranchBuilder().build()
        );

        assertThat(collaborator).isEqualTo(collaborator2);
    }

    @Test
    void itShouldReturnFalseWhen2CollaboratorsHaveTheDifferentIds() {
        var collaborator = Collaborator.createNew(
                new CollaboratorId(),
                "bla@mail.com",
                "pirlonzio",
                Collections.emptySet(),
                new OfficeBranchBuilder().build()
        );
        var collaborator2 = Collaborator.createNew(
                new CollaboratorId(),
                "bla@mail.com",
                "pirlonzio",
                Collections.emptySet(),
                new OfficeBranchBuilder().build()
        );

        assertThat(collaborator).isNotEqualTo(collaborator2);
    }

    @Test
    void itShouldReturnOnlyActiveRoles() {
        var role1 = new RoleBuilder()
                .withPermissions(ImmutableSet.of(Permission.create(Access.READ, Resource.MEMBERSHIP)))
                .build();
        role1.markAsDeleted();
        var role2 = new RoleBuilder().build();

        var collaborator = new CollaboratorBuilder()
                .addRole(role1)
                .addRole(role2)
                .build();

        assertThat(collaborator.roles()).containsExactly(role2);
    }

    @Test
    void itShouldReturnTrueWhenCollaboratorHasPermissionForActiveRole() {
        var role1 = new RoleBuilder()
                .withPermissions(ImmutableSet.of(Permission.create(Access.READ, Resource.MEMBERSHIP)))
                .build();
        var role2 = new RoleBuilder().build();

        var collaborator = new CollaboratorBuilder()
                .addRole(role1)
                .addRole(role2)
                .build();

        assertThat(collaborator.hasPermission(Permission.create(Access.READ, Resource.MEMBERSHIP)))
                .isTrue();
    }

    @Test
    void itShouldReturnFalseWhenCollaboratorHasPermissionForAnInactiveRole() {
        var role1 = new RoleBuilder()
                .withPermissions(ImmutableSet.of(Permission.create(Access.READ, Resource.MEMBERSHIP)))
                .build();
        role1.markAsDeleted();
        var role2 = new RoleBuilder().build();

        var collaborator = new CollaboratorBuilder()
                .addRole(role1)
                .addRole(role2)
                .build();

        assertThat(collaborator.hasPermission(Permission.create(Access.READ, Resource.MEMBERSHIP)))
                .isFalse();
    }
}
