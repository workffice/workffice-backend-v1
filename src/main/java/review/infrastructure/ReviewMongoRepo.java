package review.infrastructure;

import io.vavr.control.Option;
import io.vavr.control.Try;
import review.domain.review.Review;
import review.domain.review.ReviewRepository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewMongoRepo implements ReviewRepository {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Review> findByOfficeId(String officeId, Integer from, Integer limit) {
        var criteria = Criteria.where("officeId").is(officeId);
        var query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "created"))
                .skip(from)
                .limit(limit);
        return mongoTemplate.find(query, Review.class);
    }

    @Override
    public Option<Review> find(String renterEmail, String officeId) {
        var criteria = Criteria.where("renterEmail").is(renterEmail).and("officeId").is(officeId);
        return Option.of(mongoTemplate.findOne(Query.query(criteria), Review.class));
    }

    @Override
    public Try<Void> store(Review review) {
        return Try.run(() -> mongoTemplate.save(review));
    }
}
