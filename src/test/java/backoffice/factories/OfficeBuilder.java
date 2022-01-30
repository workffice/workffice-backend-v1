package backoffice.factories;

import backoffice.domain.office.Image;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.Privacy;
import backoffice.domain.office.Tables;
import backoffice.domain.office_branch.OfficeBranch;
import com.github.javafaker.Faker;

public class OfficeBuilder {
    private final Faker faker = new Faker();
    private OfficeId id = new OfficeId();
    private String name = faker.name().name();
    private String description = faker.lorem().paragraph();
    private Integer capacity = faker.number().randomDigit();
    private Integer price = faker.number().randomDigit();
    private Image image = new Image(faker.internet().image());
    private Privacy privacy = Privacy.PRIVATE;
    private Tables tables = Tables.create(10, 10);
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();

    public OfficeBuilder withOfficeBranch(OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }

    public OfficeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public OfficeBuilder withDescription(String desc) {
        this.description = desc;
        return this;
    }

    public OfficeBuilder withPrice(Integer price) {
        this.price = price;
        return this;
    }

    public OfficeBuilder withCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }

    public OfficeBuilder withImage(Image image) {
        this.image = image;
        return this;
    }

    public OfficeBuilder withPrivacy(Privacy privacy) {
        this.privacy = privacy;
        return this;
    }

    public OfficeBuilder withTables(Tables tables) {
        this.tables = tables;
        return this;
    }

    public Office build() {
        return Office.create(
                id,
                name,
                description,
                capacity,
                price,
                image,
                privacy,
                officeBranch,
                tables
        ).get();
    }
}
