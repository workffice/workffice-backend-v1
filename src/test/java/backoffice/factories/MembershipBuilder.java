package backoffice.factories;

import backoffice.domain.membership.Membership;
import backoffice.domain.membership.MembershipId;
import backoffice.domain.office_branch.OfficeBranch;
import com.github.javafaker.Faker;

public class MembershipBuilder {
    private Faker faker = Faker.instance();
    private MembershipId id = new MembershipId();
    private String name = faker.rickAndMorty().character();
    private String description = faker.lorem().paragraph();
    private Integer pricePerMonth = faker.number().numberBetween(10, 1500);
    private OfficeBranch officeBranch = new OfficeBranchBuilder().build();

    public MembershipBuilder withOfficeBranch(OfficeBranch officeBranch) {
        this.officeBranch = officeBranch;
        return this;
    }

    public Membership build() {
        return Membership.createNew(
                id,
                name,
                description,
                pricePerMonth,
                officeBranch
        );
    }
}
