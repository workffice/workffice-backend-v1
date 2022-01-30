package review.application.dto;

import lombok.Value;

@Value(staticConstructor = "of")
public class ReviewInfo {
    String officeId;
    Integer stars;
    String comment;
    String renterEmail;
}
