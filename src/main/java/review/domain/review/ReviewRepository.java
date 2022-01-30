package review.domain.review;

import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public interface ReviewRepository {

    List<Review> findByOfficeId(String officeId, Integer from, Integer limit);

    Option<Review> find(String renterEmail, String officeId);

    Try<Void> store(Review review);
}
