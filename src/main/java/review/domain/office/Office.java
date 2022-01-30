package review.domain.office;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Office {
    @MongoId
    private final String  id;
    private final String  officeBranchId;
    private final Integer totalStars;
    private final Integer totalVotes;

    public static Office create(String id, String officeBranchId) {
        return new Office(id, officeBranchId, 0, 0);
    }

    public static Office create(String id, String officeBranchId, Integer totalStars, Integer totalVotes) {
        return new Office(id, officeBranchId, totalStars, totalVotes);
    }

    public Office addVote(Integer stars) {
        return new Office(id, officeBranchId, totalStars + stars, totalVotes + 1);
    }

    public Integer starsAverage() {
        return totalStars / totalVotes;
    }

    public Integer totalVotes() { return totalVotes; }
}
