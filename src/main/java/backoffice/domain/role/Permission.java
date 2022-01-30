package backoffice.domain.role;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "permissions")
@NoArgsConstructor
@EqualsAndHashCode(of = {"resource", "access"})
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Access access;
    @Enumerated(EnumType.STRING)
    private Resource resource;
    
    private Permission(Access access, Resource resource) {
        this.access = access;
        this.resource = resource;
    }
    
    public static Permission create(Access access, Resource resource) {
        return new Permission(access, resource);
    }
    
    public Long id() { return id; }

    public Access access() { return access; }
    
    public Resource resource() { return resource; }
}
