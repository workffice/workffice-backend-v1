package backoffice.factories;

import backoffice.domain.equipment.Equipment;
import backoffice.domain.equipment.EquipmentCategory;
import backoffice.domain.equipment.EquipmentId;
import backoffice.domain.office_branch.OfficeBranch;
import com.github.javafaker.Faker;

public class EquipmentBuilder {

    private final Faker faker = new Faker();

    private EquipmentId id = new EquipmentId();
    private String name = faker.name().name();
    private EquipmentCategory equipmentCategory = EquipmentCategory.TECHNOLOGY;
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();

    public EquipmentBuilder withOfficeBranch (OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }

    public Equipment build() {
        return Equipment.create(
                id,
                name,
                equipmentCategory,
                officeBranch
        );
    }
}
