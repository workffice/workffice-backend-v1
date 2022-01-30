package search.factories;

import com.github.javafaker.Faker;
import search.domain.Office;
import search.domain.OfficeBranch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfficeBranchBuilder {
    private Faker faker = Faker.instance();
    private String id = UUID.randomUUID().toString();
    private String name = faker.name().name();
    private String phone = faker.phoneNumber().phoneNumber();
    private String province = faker.address().state();
    private String city = faker.address().city();
    private String street = faker.address().streetAddress();
    private List<Office> offices = new ArrayList<>();
    private List<String> images = new ArrayList<>();

    public static OfficeBranchBuilder builder() {
        return new OfficeBranchBuilder();
    }

    public OfficeBranch build() {
        var officeBranch = OfficeBranch.create(
                id,
                name,
                phone,
                province,
                city,
                street,
                images
        );
        offices.forEach(officeBranch::addNewOffice);
        return officeBranch;
    }

    public OfficeBranchBuilder withImages(List<String> images) {
        this.images = images;
        return this;
    }

    public OfficeBranchBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public OfficeBranchBuilder addOffice(Office office) {
        this.offices.add(office);
        return this;
    }

    public OfficeBranchBuilder withId(String id) {
        this.id = id;
        return this;
    }
}
