package review.infrastructure;

import review.factories.ReviewBuilder;
import server.WorkfficeApplication;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestReviewMongoRepo {
    @Autowired
    ReviewMongoRepo reviewMongoRepo;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        var collection = mongoTemplate.getCollection("reviews");
        collection.drop();
    }

    @Test
    void itShouldReturnAllReviewsRelatedWithOffice() {
        var review1 = new ReviewBuilder()
                .withCreated(LocalDateTime.of(2021, 11, 10, 18, 0))
                .withOfficeId("12").build();
        var review2 = new ReviewBuilder()
                .withCreated(LocalDateTime.of(2021, 11, 9, 18, 0))
                .withOfficeId("12").build();
        var review3 = new ReviewBuilder()
                .withCreated(LocalDateTime.of(2021, 11, 10, 18, 30))
                .withOfficeId("12").build();
        var review4 = new ReviewBuilder()
                .withCreated(LocalDateTime.of(2021, 11, 8, 9, 0))
                .withOfficeId("12").build();

        reviewMongoRepo.store(review1).get();
        reviewMongoRepo.store(review2).get();
        reviewMongoRepo.store(review3).get();
        reviewMongoRepo.store(review4).get();

        var reviews = reviewMongoRepo.findByOfficeId("12", 0, 2);
        var reviews2 = reviewMongoRepo.findByOfficeId("12", 2, 2);

        assertThat(reviews.size()).isEqualTo(2);
        assertThat(reviews).containsExactly(review3, review1);
        assertThat(reviews2.size()).isEqualTo(2);
        assertThat(reviews2).containsExactly(review2, review4);
    }

    @Test
    void itShouldReturnReviewRelatedWithRenterEmail() {
        var review1 = new ReviewBuilder()
                .withOfficeId("123")
                .withRenterEmail("napoleon@email.com")
                .build();
        var review2 = new ReviewBuilder()
                .withOfficeId("123")
                .withRenterEmail("john@wick.com")
                .build();

        reviewMongoRepo.store(review1).get();
        reviewMongoRepo.store(review2).get();

        var review = reviewMongoRepo.find("napoleon@email.com", "123");

        assertThat(review.isDefined()).isTrue();
        assertThat(review.get()).isEqualTo(review1);
    }
}
