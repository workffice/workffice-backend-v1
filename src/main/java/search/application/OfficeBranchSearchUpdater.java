package search.application;

import backoffice.domain.office_branch.OfficeBranchUpdatedEvent;
import search.domain.OfficeBranchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchSearchUpdater {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchSearchUpdater(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void updateOfficeBranch(OfficeBranchUpdatedEvent event) {
        officeBranchRepo
                .findById(event.getId())
                .map(officeBranch -> officeBranch.update(
                        event.getName(),
                        event.getPhone(),
                        event.getProvince(),
                        event.getCity(),
                        event.getStreet(),
                        event.getImageUrls()
                ))
                .peek(officeBranchRepo::update);
    }
}
