package booking.factories;

import booking.domain.inactivity.Inactivity;
import booking.domain.office.Office;
import booking.domain.office.OfficeId;
import booking.domain.office.privacy.Privacy;
import booking.domain.office.privacy.SharedOffice;
import com.github.javafaker.Faker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OfficeBuilder {
    Faker faker = Faker.instance();
    private OfficeId id = new OfficeId();
    private String officeBranchId = UUID.randomUUID().toString();
    private List<Inactivity> inactivities = new ArrayList<>();
    private String name = faker.name().name();
    private Integer price = faker.number().numberBetween(1, 150);
    private Privacy privacy = new SharedOffice(10, 10);

    public OfficeBuilder withPrice(Integer price) {
        this.price = price;
        return this;
    }

    public OfficeBuilder withPrivacy(Privacy privacy) {
        this.privacy = privacy;
        return this;
    }

    public Office build() {
        var office = Office.create(
                id,
                officeBranchId,
                name,
                price,
                privacy
        );
        inactivities.forEach(office::addInactivity);
        return office;
    }
}
