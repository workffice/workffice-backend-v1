package backoffice.factories;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.service.Service;
import backoffice.domain.service.ServiceCategory;
import backoffice.domain.service.ServiceId;
import com.github.javafaker.Faker;

public class ServiceBuilder {

    private final Faker faker = new Faker();

    private ServiceId id = new ServiceId();
    private String name = faker.name().name();
    private ServiceCategory serviceCategory = ServiceCategory.FOOD;
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();

    public ServiceBuilder withOfficeBranch(OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }

    public Service build() {
        return Service.create(
                id,
                name,
                serviceCategory,
                officeBranch
        );
    }
}
