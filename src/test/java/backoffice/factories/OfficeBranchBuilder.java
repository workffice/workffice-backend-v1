package backoffice.factories;

import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.Location;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.office_holder.OfficeHolder;
import com.github.javafaker.Faker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OfficeBranchBuilder {
    private OfficeBranchId id;
    private String name, description, phone;
    private LocalDate created;
    private List<Image> images;
    private Location location;
    private OfficeHolder owner;

    public OfficeBranchBuilder() {
        Faker faker = new Faker();
        id = new OfficeBranchId();
        name = faker.name().name();
        description = faker.lorem().paragraph();
        phone = faker.phoneNumber().phoneNumber();
        created = LocalDate.now();
        owner = new OfficeHolderBuilder().build();
        location = new LocationBuilder().build();
        images = new ArrayList<>();
    }

    public OfficeBranchBuilder withId(OfficeBranchId id) {
        this.id = id;
        return this;
    }

    public OfficeBranchBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public OfficeBranchBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public OfficeBranchBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public OfficeBranchBuilder withOwner(OfficeHolder owner) {
        this.owner = owner;
        return this;
    }

    public OfficeBranchBuilder withLocation(Location location) {
        this.location = location;
        return this;
    }

    public OfficeBranchBuilder withImages(List<Image> images) {
        this.images = images;
        return this;
    }

    public OfficeBranch build() {
        return new OfficeBranch(
                id,
                owner,
                name,
                description,
                phone,
                created,
                images,
                location
        );
    }
}
