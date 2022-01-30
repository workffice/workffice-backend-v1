package review.domain.review;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import review.application.dto.ReviewResponse;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "reviews")
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(exclude = {"created"})
public class Review {
    @MongoId
    private final String  id;
    private final String  comment;
    private final Integer stars;
    private final String  officeBranchId;
    private final String  officeId;
    private final String  renterEmail;
    private final LocalDateTime created;

    public static Try<Review> create(
            String id,
            String comment,
            Integer stars,
            String officeBranchId,
            String officeId,
            String renterEmail
    ) {
        if (stars > 0 && stars <= 5)
            return Try.success(new Review(
                    id,
                    comment,
                    stars,
                    officeBranchId,
                    officeId,
                    renterEmail,
                    LocalDateTime.now(Clock.systemUTC()).withSecond(0).withNano(0)
            ));
        return Try.failure(new IllegalArgumentException("Stars must be a number between 1 and 5"));
    }

    public ReviewResponse toResponse() {
        return ReviewResponse.of(
                comment,
                stars,
                renterEmail,
                created
        );
    }
}
