package search.factories;

import com.github.javafaker.Faker;
import search.domain.Office;
import search.domain.OfficePrivacy;

import java.util.UUID;

public class OfficeBuilder {
    private Faker         faker            = Faker.instance();
    private String        id               = UUID.randomUUID().toString();
    private String        name             = faker.name().name();
    private Integer       price            = faker.number().numberBetween(0, 5000);
    private Integer       capacity         = faker.number().numberBetween(0, 250);
    private Integer       tablesQuantity   = faker.number().numberBetween(0, 50);
    private Integer       capacityPerTable = faker.number().numberBetween(0, 20);
    private OfficePrivacy privacy          = OfficePrivacy.SHARED;

    public static OfficeBuilder builder() {
        return new OfficeBuilder();
    }

    public Office build() {
        return Office.create(
                id,
                name,
                price,
                capacity,
                tablesQuantity,
                capacityPerTable,
                privacy
        );
    }

    public OfficeBuilder withPrivacy(OfficePrivacy privacy) {
        this.privacy = privacy;
        return this;
    }

    public OfficeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public OfficeBuilder withCapacity(Integer capacity) {
        this.capacity = capacity;
        return this;
    }
}
