package backoffice.infrastructure;

import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeHolderBuilder;
import io.vavr.control.Option;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestOfficeHolderMySQLRepo {
    @Autowired
    OfficeHolderMySQLRepo officeHolderRepo;
    @Autowired
    OfficeBranchMySQLRepo officeBranchRepo;

    @Test
    void itShouldStoreOfficeHolderIntoDb() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();

        officeHolderRepo.store(officeHolder);

        OfficeHolder officeHolderSaved = officeHolderRepo.findById(officeHolder.id()).get();
        assertThat(officeHolderSaved).isEqualTo(officeHolder);
    }

    @Test
    void itShouldReturnOfficeHolderRelatedWithOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .build();
        officeHolderRepo.store(officeHolder);
        officeBranchRepo.store(officeBranch);

        var officeBranchSaved = officeBranchRepo.findById(officeBranch.id()).get();
        var officeHolderReceived = officeHolderRepo
                .findByOfficeBranch(officeBranchSaved);

        assertThat(officeHolderReceived.get()).isEqualTo(officeHolder);
    }

    @Test
    void itShouldReturnEmptyWhenOfficeBranchIsNotStored() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .build();

        var officeHolderReceived = officeHolderRepo.findByOfficeBranch(officeBranch);

        assertThat(officeHolderReceived.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnOfficeHolderWithEmailSpecified() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);

        Option<OfficeHolder> officeHolderFound = officeHolderRepo.find(officeHolder.email());

        assertThat(officeHolderFound.isDefined()).isTrue();
        assertThat(officeHolderFound.get().id()).isEqualTo(officeHolder.id());
    }
}
