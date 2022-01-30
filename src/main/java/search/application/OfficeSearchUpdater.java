package search.application;

import backoffice.domain.office.OfficeUpdatedEvent;
import search.domain.OfficeBranchRepository;
import search.domain.OfficePrivacy;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OfficeSearchUpdater {
    private final OfficeBranchRepository officeBranchRepo;

    public OfficeSearchUpdater(OfficeBranchRepository officeBranchRepo) {
        this.officeBranchRepo = officeBranchRepo;
    }

    @EventListener
    public void updateOffice(OfficeUpdatedEvent event) {
        officeBranchRepo
                .findById(event.getOfficeBranchId())
                .map(officeBranch -> {
                    officeBranch.offices().forEach(office -> {
                        if (office.id().equals(event.getId()))
                            office.update(
                                    event.getName(),
                                    event.getPrice(),
                                    event.getCapacity(),
                                    event.getTablesQuantity(),
                                    event.getCapacityPerTable(),
                                    OfficePrivacy.valueOf(event.getPrivacy())
                            );
                    });
                    return officeBranch;
                }).peek(officeBranchRepo::update);
    }
}
