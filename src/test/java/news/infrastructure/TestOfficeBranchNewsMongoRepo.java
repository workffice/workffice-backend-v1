package news.infrastructure;

import news.domain.OfficeBranch;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestOfficeBranchNewsMongoRepo {
    @Autowired
    OfficeBranchNewsMongoRepo officeBranchRepo;

    @Test
    void itShouldStoreOfficeBranch() {
        var officeBranch = new OfficeBranch("1");
        officeBranch.addRenterEmail("pp@mail.com");

        officeBranchRepo.store(officeBranch);

        var officeBranchStored = officeBranchRepo.findById("1").get();
        assertThat(officeBranchStored.renterEmails()).containsExactly("pp@mail.com");
    }

    @Test
    void itShouldUpdateOfficeBranch() {
        var officeBranch = new OfficeBranch("1");
        officeBranch.addRenterEmail("pp@mail.com");
        officeBranchRepo.store(officeBranch);

        officeBranch.addRenterEmail("some@mail.com");
        officeBranch.addRenterEmail("napoleon@mail.com");
        officeBranchRepo.update(officeBranch);

        var officeBranchStored = officeBranchRepo.findById("1").get();
        assertThat(officeBranchStored.renterEmails()).containsExactlyInAnyOrder(
                "pp@mail.com",
                "some@mail.com",
                "napoleon@mail.com"
        );
    }
}
