package review.application;

import review.application.dto.OfficeBranchReviewResponse;
import review.domain.office.Office;
import review.domain.office.OfficeRepository;

import org.springframework.stereotype.Service;

@Service
public class OfficeBranchReviewCalculator {
    private final OfficeRepository officeRepo;

    public OfficeBranchReviewCalculator(OfficeRepository officeRepo) {
        this.officeRepo = officeRepo;
    }

    public OfficeBranchReviewResponse calculate(String officeBranchId) {
        var offices = officeRepo
                .findByOfficeBranchId(officeBranchId);
        if (offices.isEmpty())
            return OfficeBranchReviewResponse.of(officeBranchId, 0, 0);
        var officeBranchStarsAverage = offices
                .stream()
                .map(Office::starsAverage)
                .reduce(0, Integer::sum) / offices.size();
        var officeBranchTotalVotes = offices
                .stream()
                .map(Office::totalVotes)
                .reduce(0, Integer::sum);
        return OfficeBranchReviewResponse.of(officeBranchId, officeBranchTotalVotes, officeBranchStarsAverage);
    }
}
