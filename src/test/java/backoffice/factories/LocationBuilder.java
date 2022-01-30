package backoffice.factories;

import backoffice.domain.office_branch.Location;
import com.github.javafaker.Faker;

public class LocationBuilder {
    private final Faker faker = new Faker();
    private String province = faker.country().name();
    private String city = faker.country().capital();
    private String street = faker.address().streetName();
    private String zipCode = faker.address().zipCode();
    
    public LocationBuilder withProvince(String province) {
        this.province = province;
        return this;
    }
    
    public LocationBuilder withCity(String city) {
        this.city = city;
        return this;
    }
    
    public LocationBuilder withStreet(String street) {
        this.street = street;
        return this;
    }
    
    public LocationBuilder withZipCode(String zipCode) {
        this.zipCode = zipCode;
        return this;
    }
    
    public Location build() {
        return new Location(province, city, street, zipCode);
    }
}
