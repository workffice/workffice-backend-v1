package review.application;

import authentication.application.AuthUserValidator;
import booking.application.booking.BookingExistsResolver;
import booking.domain.office.OfficeId;
import io.vavr.control.Either;
import review.application.dto.ReviewError;
import review.application.dto.ReviewInfo;
import review.domain.review.Review;
import review.domain.review.ReviewRepository;

import org.springframework.stereotype.Service;

@Service
public class ReviewCreator {
    private final ReviewRepository      reviewRepo;
    private final AuthUserValidator     authUserValidator;
    private final BookingExistsResolver bookingExistsResolver;
    private final OfficeReviewUpdater   officeReviewUpdater;

    public ReviewCreator(
            ReviewRepository      reviewRepo,
            AuthUserValidator     authUserValidator,
            BookingExistsResolver bookingExistsResolver,
            OfficeReviewUpdater   officeReviewUpdater
    ) {
        this.reviewRepo            = reviewRepo;
        this.authUserValidator     = authUserValidator;
        this.bookingExistsResolver = bookingExistsResolver;
        this.officeReviewUpdater   = officeReviewUpdater;
    }

    public Either<ReviewError, Void> create(String id, String officeBranchId, ReviewInfo info) {
        if (!authUserValidator.isSameUserAsAuthenticated(info.getRenterEmail()))
            return Either.left(ReviewError.REVIEW_FORBIDDEN);
        var existentReview = reviewRepo.find(info.getRenterEmail(), info.getOfficeId());
        if (existentReview.isDefined())
            return Either.left(ReviewError.REVIEW_ALREADY_CREATED);
        try {
            var renterHasBookedForOffice = bookingExistsResolver.bookingExists(
                    info.getRenterEmail(),
                    OfficeId.fromString(info.getOfficeId())
            );
            if (!renterHasBookedForOffice)
                return Either.left(ReviewError.NO_BOOKING);
        } catch (IllegalArgumentException e) {
            return Either.left(ReviewError.INVALID_REVIEW_INFO);
        }
        return Review.create(
                id,
                info.getComment(),
                info.getStars(),
                officeBranchId,
                info.getOfficeId(),
                info.getRenterEmail())
                .toEither(ReviewError.INVALID_REVIEW_INFO)
                .flatMap(review -> reviewRepo
                        .store(review)
                        .onSuccess(v -> officeReviewUpdater
                                .updateOfficeReviews(info.getOfficeId(), officeBranchId, info.getStars()))
                        .toEither(ReviewError.DB_ERROR)
                );
    }
}
