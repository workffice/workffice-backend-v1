package search.application;

import backoffice.domain.office.OfficeDeletedEvent;
import search.domain.OfficeBranchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeSearchDeleter {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeSearchDeleter(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void deleteOffice(OfficeDeletedEvent event) {
        officeBranchRepo
                .findById(event.getOfficeBranchId())
                .map(officeBranch -> {
                    officeBranch.removeOffice(event.getOfficeId());
                    return officeBranch;
                })
                .peek(officeBranchRepo::update);
    }
}
