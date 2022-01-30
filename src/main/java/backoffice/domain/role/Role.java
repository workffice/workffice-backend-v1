package backoffice.domain.role;

import backoffice.application.dto.role.RoleResponse;
import backoffice.domain.office_branch.OfficeBranch;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"id", "name", "permissions"})
public class Role {
    @EmbeddedId
    private RoleId id;
    @Column
    private String name;
    @Column
    boolean deleted;
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permission",
            joinColumns = {@JoinColumn(name = "role_id")},
            inverseJoinColumns = {@JoinColumn(name = "permission_id")}
    )
    private Set<Permission> permissions;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;

    public static Role create(RoleId id, String name, Set<Permission> permissions, OfficeBranch officeBranch) {
        return new Role(id, name, false, permissions, officeBranch);
    }

    public boolean hasAccessTo(Permission permission) {
        Predicate<Permission> permHasWriteAccessForSameResource = perm ->
                (perm.resource().equals(permission.resource()) && perm.access().equals(Access.WRITE));
        return permissions
                .stream()
                .anyMatch(perm -> perm.equals(permission) || permHasWriteAccessForSameResource.test(perm));
    }

    public RoleId id() { return id; }

    public Set<Permission> permissions() { return permissions; }

    public String name() { return name; }

    public OfficeBranch officeBranch() { return officeBranch; }

    public RoleResponse toResponse() {
        var permissionResponses = permissions
                .stream()
                .map(permission -> RoleResponse.PermissionResponse.of(
                        permission.resource().name(),
                        permission.access().name())
                ).collect(Collectors.toSet());
        return RoleResponse.of(id.toString(), name, permissionResponses);
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public boolean isActive() {
        return !deleted;
    }
}
