package review.application;

import review.application.dto.ReviewResponse;
import review.domain.review.Review;
import review.domain.review.ReviewRepository;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ReviewFinder {
    private final ReviewRepository reviewRepo;

    public ReviewFinder(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    public List<ReviewResponse> find(String officeId, Pageable pageable) {
        return reviewRepo
                .findByOfficeId(officeId, (int) pageable.getOffset(), pageable.getPageSize())
                .stream()
                .map(Review::toResponse)
                .collect(Collectors.toList());

    }
}
