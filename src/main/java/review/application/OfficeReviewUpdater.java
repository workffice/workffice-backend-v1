package review.application;

import io.vavr.control.Option;
import review.domain.office.Office;
import review.domain.office.OfficeRepository;

import org.springframework.stereotype.Service;

@Service
public class OfficeReviewUpdater {

    private final OfficeRepository officeRepo;

    public OfficeReviewUpdater(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    /**
     *  This implementation needs optimistic locking in case
     *  two reviews for the same office are done in the same time
     *  When that case happens we will probably lose one review and
     *  we the average reviews will be bad calculated
     */
    public void updateOfficeReviews(String officeId, String officeBranchId, Integer stars) {
        officeRepo
                .findById(officeId)
                .orElse(() -> Option.of(Office.create(officeId, officeBranchId)))
                .map(office -> office.addVote(stars))
                .map(officeRepo::save);

    }
}
