package review.factories;

import com.github.javafaker.Faker;
import review.domain.review.Review;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewBuilder {
    Faker faker = Faker.instance();
    private String id = UUID.randomUUID().toString();
    private String comment = faker.lorem().fixedString(300);
    private Integer stars = faker.number().numberBetween(1, 5);
    private String officeBranchId = UUID.randomUUID().toString();
    private String officeId = UUID.randomUUID().toString();
    private String renterEmail = faker.internet().emailAddress();
    private LocalDateTime created = LocalDateTime.now(Clock.systemUTC());

    public ReviewBuilder withCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public ReviewBuilder withOfficeId(String officeId) {
        this.officeId = officeId;
        return this;
    }

    public ReviewBuilder withRenterEmail(String email) {
        this.renterEmail = email;
        return this;
    }

    public Review build() {
        return new Review(id, comment, stars, officeBranchId, officeId, renterEmail, created);
    }
}
