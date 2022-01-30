package review.domain;

import review.domain.review.Review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReview {

    @Test
    void itShouldReturnFailureWhenStarsIsGreaterThan5() {
        var reviewOrError = Review.create(
                "1",
                "Super comment",
                6,
                "1",
                "2",
                "pepito@email.com"
        );

        assertThat(reviewOrError.isFailure()).isTrue();
    }

    @Test
    void itShouldReturnFailureWhenStarsIsEqualLessThan0() {
        var reviewOrError = Review.create(
                "1",
                "Super comment",
                0,
                "1",
                "2",
                "pepito@email.com"
        );

        assertThat(reviewOrError.isFailure()).isTrue();
    }

    @Test
    void itShouldReturnReview() {
        var reviewOrError = Review.create(
                "1",
                "Super comment",
                5,
                "1",
                "2",
                "pepito@email.com"
        );

        assertThat(reviewOrError.isSuccess()).isTrue();
    }
}
