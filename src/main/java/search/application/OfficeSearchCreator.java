package search.application;

import backoffice.domain.office.OfficeCreatedEvent;
import search.domain.Office;
import search.domain.OfficeBranchRepository;
import search.domain.OfficePrivacy;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeSearchCreator {

    private final OfficeBranchRepository officeBranchRepo;

    public OfficeSearchCreator(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void createOffice(OfficeCreatedEvent event) {
        var office = Office.create(
                event.getId(),
                event.getName(),
                event.getPrice(),
                event.getCapacity(),
                event.getTablesQuantity(),
                event.getCapacityPerTable(),
                OfficePrivacy.valueOf(event.getPrivacy())
        );
        officeBranchRepo
                .findById(event.getOfficeBranchId())
                .peek(officeBranch -> {
                    officeBranch.addNewOffice(office);
                    officeBranchRepo.update(officeBranch);
                });
    }
}
