package backoffice.factories;

import backoffice.domain.collaborator.Collaborator;
import backoffice.domain.collaborator.CollaboratorId;
import backoffice.domain.collaborator.Status;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Role;
import com.github.javafaker.Faker;
import com.google.common.collect.Sets;

import java.time.LocalDate;
import java.util.Set;

public class CollaboratorBuilder {
    private Faker faker = new Faker();
    private CollaboratorId id = new CollaboratorId();
    private String email = faker.internet().emailAddress();
    private String name = faker.name().name();
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();
    private Set<Role> roles = Sets.newHashSet();
    private Status status = Status.PENDING;
    private LocalDate created = LocalDate.now();

    public CollaboratorBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CollaboratorBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    public CollaboratorBuilder withOfficeBranch(OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }

    public CollaboratorBuilder addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public Collaborator build() {
        return new Collaborator(
                id,
                email,
                name,
                roles,
                officeBranch,
                status,
                created
        );
    }
}
