package backoffice.domain.service;

import backoffice.application.dto.service.ServiceResponse;
import backoffice.domain.office_branch.OfficeBranch;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "service")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name", "category"})
public class Service {
    @EmbeddedId
    private ServiceId id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    private ServiceCategory category;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;

    public static Service create(
            ServiceId id,
            String name,
            ServiceCategory serviceCategory,
            OfficeBranch officeBranch) {
        return new Service(
                id,
                name,
                serviceCategory,
                officeBranch);
    }

    public ServiceId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public OfficeBranch officeBranch() {
        return officeBranch;
    }

    public ServiceResponse toResponse() {
        return ServiceResponse.of(id.toString(), name, category.toString());
    }
}

