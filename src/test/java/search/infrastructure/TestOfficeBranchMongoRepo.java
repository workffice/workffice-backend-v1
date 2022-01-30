package search.infrastructure;

import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;
import search.domain.OfficeBranch;
import search.domain.OfficePrivacy;
import search.domain.spec.Specification;
import search.factories.OfficeBranchBuilder;
import search.factories.OfficeBuilder;
import server.WorkfficeApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestOfficeBranchMongoRepo {
    @Autowired
    OfficeBranchMongoRepo officeBranchMongoRepo;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        var collection = mongoTemplate.getCollection("office_branches");
        collection.drop();
    }

    @Test
    void itShouldStoreOfficeBranchIntoDatabase() {
        String id = UUID.randomUUID().toString();
        var officeBranch = OfficeBranch.create(
                id,
                "Monumental",
                "2513749180",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        );

        officeBranchMongoRepo.store(officeBranch);

        var officeBranchStored = mongoTemplate.findById(id, OfficeBranch.class);
        assertThat(officeBranchStored).isEqualTo(officeBranch);
    }

    @Test
    void itShouldUpdateOfficeBranchInDatabase() {
        String id = UUID.randomUUID().toString();
        var officeBranch = OfficeBranchBuilder.builder()
                .withId(id)
                .build();
        officeBranchMongoRepo.store(officeBranch);
        var office = OfficeBuilder.builder().build();
        officeBranch.addNewOffice(office);

        officeBranchMongoRepo.update(officeBranch);

        var officeBranchUpdated = mongoTemplate.findById(id, OfficeBranch.class);
        assertThat(officeBranchUpdated).isNotNull();
        assertThat(officeBranchUpdated.offices()).size().isEqualTo(1);
    }

    @Test
    void itShouldFindOfficeBranchById() {
        String id = UUID.randomUUID().toString();
        var officeBranch = OfficeBranchBuilder.builder()
                .withId(id)
                .build();
        officeBranchMongoRepo.store(officeBranch);

        Option<OfficeBranch> officeBranchFound = officeBranchMongoRepo.findById(id);

        assertThat(officeBranchFound.isDefined()).isTrue();
        assertThat(officeBranchFound.get()).isEqualTo(officeBranch);
    }

    @Test
    void itShouldReturnEmptyOfficeBranch() {
        String id = UUID.randomUUID().toString();

        Option<OfficeBranch> officeBranchFound = officeBranchMongoRepo.findById(id);

        assertThat(officeBranchFound.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnTheAmountOfOfficeBranchesThatMatchTheSpecification() {
        var officeBranch = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .withName("La pelela")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        var officeBranchHasName = Specification.eq("name", "River Camp");
        var officeBranchContainsSharedOffice = Specification.anyMatch(
                "offices",
                Specification.eq("privacy", OfficePrivacy.SHARED.name())
        );

        Long quantity = officeBranchMongoRepo
                .count(ImmutableList.of(officeBranchHasName, officeBranchContainsSharedOffice));

        assertThat(quantity).isEqualTo(3);
    }

    @Test
    void itShouldReturnTheTotalQuantityOfOfficeBranchesWhenNoSpecIsProvided() {
        var officeBranch = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .withName("Monumental")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .withName("El taladro")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .withName("La pelela")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        Long quantity = officeBranchMongoRepo.count(new ArrayList<>());

        assertThat(quantity).isEqualTo(4);
    }

    @Test
    void itShouldRemoveOfficeBranchFromDatabase() {
        String id = UUID.randomUUID().toString();
        var officeBranch = OfficeBranch.create(
                id,
                "BB",
                "2513749180",
                "Buenos Aires",
                "Belgrano",
                "Fake street 1234",
                Arrays.asList("image1.com", "image2.com")
        );
        officeBranchMongoRepo.store(officeBranch);

        officeBranchMongoRepo.delete(id);

        var officeBranchStored = officeBranchMongoRepo.findById(id);
        assertThat(officeBranchStored.isEmpty()).isTrue();
    }
}
