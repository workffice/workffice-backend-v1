package search.infrastructure;

import com.google.common.collect.ImmutableList;
import search.domain.OfficeBranch;
import search.domain.OfficePrivacy;
import search.domain.spec.Specification;
import search.factories.OfficeBranchBuilder;
import search.factories.OfficeBuilder;
import server.WorkfficeApplication;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestOfficeBranchMongoSearch {
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
    void itShouldReturnOfficeBranchesThatMatchWithNameSpecifiedIgnoringCase() {
        var officeBranch = OfficeBranchBuilder.builder()
                .withName("Monumental")
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .withName("Pepito")
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .withName("La pelela")
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);

        var officeBranchHasSpecName = Specification.eq("name", "moNumEntaL");
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(officeBranchHasSpecName);

        assertThat(officeBranches).size().isEqualTo(1);
        assertThat(officeBranches.get(0)).isEqualTo(officeBranch);
    }

    @Test
    void itShouldReturnOfficeBranchesThatHaveAtLeastOnePrivateOffice() {
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);

        var officeBranchContainsOfficeWithSpec = Specification.anyMatch(
                "offices",
                Specification.eq("privacy", OfficePrivacy.PRIVATE.name())
        );
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(officeBranchContainsOfficeWithSpec);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch, officeBranch3);
    }

    @Test
    void itShouldReturnTheLastTwoOfficeBranchesUsingRange() {
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        var officeBranchContainsPrivateOffice = Specification.anyMatch(
                "offices",
                Specification.eq("privacy", OfficePrivacy.PRIVATE.name())
        ).range(2, 2);
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(officeBranchContainsPrivateOffice);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch3, officeBranch4);
    }

    @Test
    void itShouldReturnAllOfficeBranchAvailableWhenNoSpecIsSpecified() {
        var officeBranch = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.SHARED).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .withName("All the night")
                .addOffice(OfficeBuilder.builder().withPrivacy(OfficePrivacy.PRIVATE).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);

        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(new ArrayList<>(), 0, 50);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch, officeBranch2);
    }

    @Test
    void itShouldReturnOfficeBranchesThatHaveAtLeastOneOfficeWithCapacityLessThan50() {
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(100).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(30).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(80).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(10).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        var officeBranchContainsOfficesWithCapacityLessThan50 = Specification.anyMatch(
                "offices",
                Specification.lt("capacity", 50)
        );
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(
                officeBranchContainsOfficesWithCapacityLessThan50);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch2, officeBranch4);
    }

    @Test
    void itShouldReturnOfficeBranchesThatHaveAtLeastOneOfficeWithCapacityGreaterThan10() {
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(50).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(5).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(11).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(8).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        var officeBranchContainsOfficesWithCapacityGreaterThan10 = Specification.anyMatch(
                "offices",
                Specification.gt("capacity", 10)
        );
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(
                officeBranchContainsOfficesWithCapacityGreaterThan10);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch, officeBranch3);
    }

    @Test
    void itShouldReturnOfficeBranchesThatHaveAtLeastOneOfficeWithCapacityBetween20And50() {
        var officeBranch = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(80).build())
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(30).build())
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(45).build())
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(OfficeBuilder.builder().withCapacity(20).build())
                .build();
        officeBranchMongoRepo.store(officeBranch);
        officeBranchMongoRepo.store(officeBranch2);
        officeBranchMongoRepo.store(officeBranch3);
        officeBranchMongoRepo.store(officeBranch4);

        var officeBranchContainsOfficesWithCapacityBetween20And50 = Specification.anyMatch(
                "offices",
                Specification.between("capacity", 20, 50)
        );
        List<OfficeBranch> officeBranches = officeBranchMongoRepo
                .search(officeBranchContainsOfficesWithCapacityBetween20And50);

        assertThat(officeBranches).size().isEqualTo(3);
        assertThat(officeBranches).containsExactlyInAnyOrder(officeBranch2, officeBranch3, officeBranch4);
    }

    @Test
    void itShouldReturnOfficeBranchesThatFulfillConditions() {
        var office = OfficeBuilder.builder()
                .withPrivacy(OfficePrivacy.SHARED)
                .withCapacity(15)
                .build();
        var officeBranch = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(office)
                .build();
        var office2 = OfficeBuilder.builder()
                .withPrivacy(OfficePrivacy.PRIVATE)
                .withCapacity(15)
                .build();
        var officeBranch2 = OfficeBranchBuilder.builder()
                .withName("River Camp")
                .addOffice(office2)
                .build();
        var office3 = OfficeBuilder.builder()
                .withPrivacy(OfficePrivacy.SHARED)
                .withCapacity(50)
                .build();
        var officeBranch3 = OfficeBranchBuilder.builder()
                .withName("La pelela")
                .addOffice(office3)
                .build();
        var office4 = OfficeBuilder.builder()
                .withPrivacy(OfficePrivacy.PRIVATE)
                .withCapacity(5)
                .build();
        var officeBranch4 = OfficeBranchBuilder.builder()
                .addOffice(office4)
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
        var officeBranchContainsOfficeWithCapacityGreaterThan10 = Specification.anyMatch(
                "offices",
                Specification.gt("capacity", 10)
        );
        List<OfficeBranch> officeBranches = officeBranchMongoRepo.search(
                ImmutableList.of(
                        officeBranchHasName,
                        officeBranchContainsSharedOffice,
                        officeBranchContainsOfficeWithCapacityGreaterThan10
                ),
                0,
                50
        );

        assertThat(officeBranches).size().isEqualTo(1);
        assertThat(officeBranches.get(0)).isEqualTo(officeBranch);
    }
}
