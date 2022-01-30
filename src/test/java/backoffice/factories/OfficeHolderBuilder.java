package backoffice.factories;

import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.office_holder.OfficeHolderId;
import com.github.javafaker.Faker;

public class OfficeHolderBuilder {
    private final Faker faker = new Faker();
    private String email = faker.internet().emailAddress();
    private OfficeHolderId id = new OfficeHolderId();
    
    public OfficeHolder build() {
        return new OfficeHolder(id, email);
    }
    
    public OfficeHolderBuilder withId(OfficeHolderId id) {
        this.id = id;
        return this;
    }
    
    public OfficeHolderBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
}
