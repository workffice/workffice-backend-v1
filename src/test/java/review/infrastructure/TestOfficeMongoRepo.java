package review.infrastructure;

import review.domain.office.Office;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestOfficeMongoRepo {
    @Autowired
    OfficeMongoRepo officeMongoRepo;

    @Test
    void itShouldReturnOfficeWithIdSpecified() {
        var office = Office.create("1", "21", 15, 3);
        officeMongoRepo.save(office);

        var officeFound = officeMongoRepo.findById("1");

        assertThat(officeFound.isDefined()).isTrue();
        assertThat(officeFound.get()).isEqualTo(office);
    }

    @Test
    void itShouldReturnAllOfficesRelatedWithOfficeBranchId() {
        var office = Office.create("1", "21", 15, 3);
        var office2 = Office.create("2", "21", 15, 3);
        var office3 = Office.create("3", "21", 15, 3);
        officeMongoRepo.save(office);
        officeMongoRepo.save(office2);
        officeMongoRepo.save(office3);

        var offices = officeMongoRepo.findByOfficeBranchId("21");

        assertThat(offices).size().isEqualTo(3);
        assertThat(offices).containsExactlyInAnyOrder(office, office2, office3);
    }
}
