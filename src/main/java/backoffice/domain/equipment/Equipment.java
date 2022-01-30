package backoffice.domain.equipment;

import backoffice.application.dto.equipment.EquipmentResponse;
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
@Table(name = "equipment")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name", "category"})
public class Equipment {
    @EmbeddedId
    private EquipmentId id;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    private EquipmentCategory category;
    @ManyToOne(fetch = FetchType.LAZY)
    private OfficeBranch officeBranch;

    public static Equipment create(
            EquipmentId id,
            String name,
            EquipmentCategory equipmentCategory,
            OfficeBranch officeBranch) {
        return new Equipment(
                id,
                name,
                equipmentCategory,
                officeBranch);
    }

    public EquipmentId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public OfficeBranch officeBranch() {
        return officeBranch;
    }

    public EquipmentResponse toResponse() {
        return EquipmentResponse.of(id.toString(), name, category.toString());

    }

}
