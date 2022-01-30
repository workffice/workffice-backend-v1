package search;

import backoffice.domain.office_branch.OfficeBranchDeletedEvent;
import search.domain.OfficeBranchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchSearchDeleter {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchSearchDeleter(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void delete(OfficeBranchDeletedEvent event) {
        officeBranchRepo.delete(event.getOfficeBranchId());
    }
}
