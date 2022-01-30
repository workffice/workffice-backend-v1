package backoffice.domain.collaborator;

import backoffice.application.dto.collaborator.CollaboratorResponse;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Role;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "collaborators")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "email", "status"})
public class Collaborator {
    @EmbeddedId
    private CollaboratorId id;
    @Column
    private String email;
    @Column
    private String name;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "collaborator_role",
            joinColumns = @JoinColumn(name = "collaborator_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column
    private LocalDate created;

    public static Collaborator createNew(
            CollaboratorId id,
            String email,
            String name,
            Set<Role> roles,
            OfficeBranch officeBranch
    ) {
        return new Collaborator(
                id,
                email,
                name,
                roles,
                officeBranch,
                Status.PENDING,
                LocalDate.now(Clock.systemUTC())
        );
    }

    public CollaboratorId id() { return id; }

    public String email() { return email; }

    public String name() { return name; }

    public Set<Role> roles() {
        return roles
                .stream()
                .filter(Role::isActive)
                .collect(Collectors.toSet());
    }

    public OfficeBranch officeBranch() { return officeBranch; }

    public boolean isActive() { return status.equals(Status.ACTIVE); }

    public void activate() { this.status = Status.ACTIVE; }

    public boolean hasPermission(Permission permission) {
        return roles.stream()
                .filter(Role::isActive)
                .anyMatch(role -> role.hasAccessTo(permission));
    }

    public CollaboratorResponse toResponse() {
        return CollaboratorResponse.of(id.toString(), email, name, status.name(), created);
    }

    public Collaborator copy(String name, Set<Role> roles) {
        return new Collaborator(id, email, name, roles, officeBranch, status, created);
    }

    public void delete() {
        this.status = Status.INACTIVE;
    }
}
