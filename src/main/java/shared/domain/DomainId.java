package shared.domain;

import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@EqualsAndHashCode(of = {"id"})
public abstract class DomainId implements Serializable {
    @Column(columnDefinition = "BINARY(16)")
    private final UUID id;

    public DomainId() {
        this.id = UUID.randomUUID();
    }

    public DomainId(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
