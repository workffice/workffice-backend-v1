package review.application.dto;

import lombok.Value;

@Value(staticConstructor = "of")
public class OfficeBranchReviewResponse {
    String  id;
    Integer totalVotes;
    Integer averageStars;
}
