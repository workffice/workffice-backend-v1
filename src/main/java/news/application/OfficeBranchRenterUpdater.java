package news.application;

import booking.domain.booking.BookingConfirmedEvent;
import news.domain.OfficeBranch;
import news.domain.OfficeBranchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchRenterUpdater {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchRenterUpdater(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void addRenterEmailToOfficeBranch(BookingConfirmedEvent event) {
        officeBranchRepo.findById(event.getOfficeBranchId())
                .peek(officeBranch -> {
                    officeBranch.addRenterEmail(event.getRenterEmail());
                    officeBranchRepo.update(officeBranch);
                })
                .onEmpty(() -> {
                    var officeBranch = new OfficeBranch(event.getOfficeBranchId());
                    officeBranch.addRenterEmail(event.getRenterEmail());
                    officeBranchRepo.store(officeBranch);
                });
    }
}
