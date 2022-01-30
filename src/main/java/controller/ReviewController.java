package controller;

import controller.response.DataResponse;
import controller.response.PaginatedResponse;
import review.application.OfficeBranchReviewCalculator;
import review.application.ReviewCreator;
import review.application.ReviewFinder;
import review.application.dto.ReviewError;
import review.application.dto.ReviewInfo;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
public class ReviewController extends BaseController {

    @Autowired ReviewCreator                creator;
    @Autowired ReviewFinder                 finder;
    @Autowired OfficeBranchReviewCalculator reviewStatsCalculator;

    @PostMapping("/api/office_branches/{officeBranchId}/reviews/")
    public ResponseEntity<?> createReview(@PathVariable String officeBranchId, @RequestBody ReviewInfo info) {
        var reviewId = UUID.randomUUID().toString();
        ResponseEntity<DataResponse> reviewForbidden = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(forbidden("REVIEW_FORBIDDEN", "You can only create a review for your email user"));
        ResponseEntity<DataResponse> reviewAlreadyCreated = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(invalid("REVIEW_ALREADY_CREATED", "You already review this office"));
        ResponseEntity<DataResponse> noBooking = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(invalid("NO_BOOKING", "You don't have bookings to review this office"));
        ResponseEntity<DataResponse> invalidReviewInfo = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(invalid(
                        "INVALID_REVIEW_INFO",
                        "Your review information is wrong, stars must be between 1 and 5"));
        return creator.create(reviewId, officeBranchId, info)
                .map(v -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated(format("/api/reviews/%s/", reviewId))))
                .getOrElseGet(error -> Match(error).of(
                        Case($(ReviewError.REVIEW_FORBIDDEN), () -> reviewForbidden),
                        Case($(ReviewError.REVIEW_ALREADY_CREATED), () -> reviewAlreadyCreated),
                        Case($(ReviewError.INVALID_REVIEW_INFO), () -> invalidReviewInfo),
                        Case($(ReviewError.NO_BOOKING), () -> noBooking)
                ));

    }

    @GetMapping("/api/office_branches/{officeBranchId}/review_stats/")
    public ResponseEntity<?> getOfficeBranchReviewStats(@PathVariable String officeBranchId) {
        return ResponseEntity.ok(
                entityResponse(reviewStatsCalculator.calculate(officeBranchId))
        );
    }

    @GetMapping("/api/offices/{officeId}/reviews/")
    public ResponseEntity<?> getOfficeReviews(@PathVariable String officeId, Pageable pageable) {
        var response = new PaginatedResponse<>(
                finder.find(officeId, pageable),
                pageable.getPageSize(),
                false,
                -1,
                pageable.getPageNumber()
        );
        return ResponseEntity.ok(response);
    }
}
