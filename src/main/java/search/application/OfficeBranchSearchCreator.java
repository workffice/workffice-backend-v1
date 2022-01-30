package search.application;

import backoffice.domain.office_branch.OfficeBranchCreatedEvent;
import search.application.dto.OfficeBranchInformation;
import search.domain.OfficeBranch;
import search.domain.OfficeBranchRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeBranchSearchCreator {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeBranchSearchCreator(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    public void createOfficeBranch(String id, OfficeBranchInformation info) {
        var officeBranch = OfficeBranch.create(
                id,
                info.getName(),
                info.getPhone(),
                info.getProvince(),
                info.getCity(),
                info.getStreet(),
                info.getImages()
        );
        officeBranchRepo.store(officeBranch);
    }

    @EventListener
    public void createOfficeBranch(OfficeBranchCreatedEvent event) {
        var officeBranchInfo = OfficeBranchInformation.of(
                event.getName(),
                event.getProvince(),
                event.getCity(),
                event.getStreet(),
                event.getPhone(),
                event.getImageUrls()
        );
        createOfficeBranch(event.getId(), officeBranchInfo);
    }
}
