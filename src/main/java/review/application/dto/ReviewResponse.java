package review.application.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value(staticConstructor = "of")
public class ReviewResponse {
    String        comment;
    Integer       stars;
    String        renterEmail;
    LocalDateTime created;
}
