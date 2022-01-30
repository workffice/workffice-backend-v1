package review.application;

import com.google.inject.internal.util.ImmutableList;
import review.domain.review.ReviewRepository;
import review.factories.ReviewBuilder;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestReviewFinder {
    ReviewRepository reviewRepo = mock(ReviewRepository.class);

    ReviewFinder finder = new ReviewFinder(reviewRepo);

    @Test
    void itShouldReturnReviewsRelatedWithOffice() {
        var review1 = new ReviewBuilder().build();
        var review2 = new ReviewBuilder().build();
        var review3 = new ReviewBuilder().build();
        when(reviewRepo.findByOfficeId("123", 0, 3))
                .thenReturn(ImmutableList.of(review1, review2, review3));

        var reviews = finder.find("123", PageRequest.of(0, 3));

        assertThat(reviews).containsExactlyInAnyOrder(
                review1.toResponse(),
                review2.toResponse(),
                review3.toResponse()
        );
    }
}
