package review.application;

import authentication.application.AuthUserValidator;
import booking.application.booking.BookingExistsResolver;
import booking.domain.office.OfficeId;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import review.application.dto.ReviewError;
import review.application.dto.ReviewInfo;
import review.domain.review.Review;
import review.domain.review.ReviewRepository;
import review.factories.ReviewBuilder;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestReviewCreator {

    ReviewRepository reviewRepo = mock(ReviewRepository.class);
    AuthUserValidator authUserValidator = mock(AuthUserValidator.class);
    BookingExistsResolver bookingExistsResolver = mock(BookingExistsResolver.class);
    OfficeReviewUpdater officeReviewUpdater = mock(OfficeReviewUpdater.class);

    ReviewCreator creator = new ReviewCreator(
            reviewRepo,
            authUserValidator,
            bookingExistsResolver,
            officeReviewUpdater
    );

    @Test
    void itShouldReturnForbiddenWhenAuthUserIsNotTheSameAsRenterEmail() {
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(false);
        var info = ReviewInfo.of("12", 2, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ReviewError.REVIEW_FORBIDDEN);
    }

    @Test
    void itShouldReturnNoBookingWhenRenterEmailHasNoBookingsForOfficeSpecified() {
        String officeId = UUID.randomUUID().toString();
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(true);
        when(reviewRepo.find("napoleon@email.com", officeId)).thenReturn(Option.none());
        when(bookingExistsResolver.bookingExists(
                "napoleon@email.com",
                OfficeId.fromString(officeId))
        ).thenReturn(false);
        var info = ReviewInfo.of(officeId, 2, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ReviewError.NO_BOOKING);
    }

    @Test
    void itShouldReturnReviewAlreadyCreatedWhenRenterAlreadyReviewTheOfficeSpecified() {
        String officeId = UUID.randomUUID().toString();
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(true);
        when(reviewRepo.find("napoleon@email.com", officeId)).thenReturn(Option.of(new ReviewBuilder().build()));
        var info = ReviewInfo.of(officeId, 2, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ReviewError.REVIEW_ALREADY_CREATED);
    }

    @Test
    void itShouldReturnInvalidReviewInfoWhenReviewInformationIsInvalid() {
        String officeId = UUID.randomUUID().toString();
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(true);
        when(bookingExistsResolver.bookingExists("napoleon@email.com", OfficeId.fromString(officeId))).thenReturn(true);
        when(reviewRepo.find("napoleon@email.com", officeId)).thenReturn(Option.none());
        var info = ReviewInfo.of(officeId, 10, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ReviewError.INVALID_REVIEW_INFO);
    }

    @Test
    void itShouldReturnInvalidInformationWhenOfficeIdIsNotUUIDFormat() {
        String officeId = "12";
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(true);
        when(bookingExistsResolver.bookingExists(eq("napoleon@email.com"), any(OfficeId.class))).thenReturn(true);
        when(reviewRepo.find("napoleon@email.com", officeId)).thenReturn(Option.none());
        var info = ReviewInfo.of(officeId, 3, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(ReviewError.INVALID_REVIEW_INFO);
    }

    @Test
    void itShouldStoreReview() {
        String officeId = UUID.randomUUID().toString();
        when(authUserValidator.isSameUserAsAuthenticated("napoleon@email.com")).thenReturn(true);
        when(bookingExistsResolver.bookingExists("napoleon@email.com", OfficeId.fromString(officeId))).thenReturn(true);
        when(reviewRepo.find("napoleon@email.com", officeId)).thenReturn(Option.none());
        when(reviewRepo.store(any())).thenReturn(Try.success(null));
        var info = ReviewInfo.of(officeId, 3, "Some comment", "napoleon@email.com");

        Either<ReviewError, Void> response = creator.create("1", "21", info);

        assertThat(response.isRight()).isTrue();
        var expectedReview = Review.create(
                "1",
                "Some comment",
                3,
                "21",
                officeId,
                "napoleon@email.com").get();
        verify(reviewRepo, times(1)).store(expectedReview);
        verify(officeReviewUpdater, times(1)).updateOfficeReviews(officeId, "21", 3);
    }
}
