package backoffice.infrastructure;

import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.Location;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.factories.LocationBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeHolderBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;
import server.WorkfficeApplication;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestOfficeBranchMySQLRepo {
    @Autowired
    OfficeBranchMySQLRepo repo;
    @Autowired
    OfficeHolderMySQLRepo officeHolderRepo;

    @Test
    void itShouldStoreOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();

        repo.store(officeBranch).get();

        OfficeBranch officeBranchStored = repo.findById(officeBranch.id()).get();
        assertThat(officeBranchStored.name()).isEqualTo(officeBranch.name());
        assertThat(officeBranchStored.created()).isEqualTo(officeBranch.created());
    }

    @Test
    void itShouldStoreImagesRelatedWithOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .withImages(List.of(new Image("imageurl"), new Image("image2url")))
                .build();

        repo.store(officeBranch).get();

        OfficeBranch officeBranchStored = repo.findById(officeBranch.id()).get();
        assertThat(officeBranchStored.images()).hasSize(2);
    }

    @Test
    void itShouldStoreLocationRelatedWithOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        Location location = new LocationBuilder().build();
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .withLocation(location)
                .build();

        repo.store(officeBranch).get();

        OfficeBranch officeBranchStored = repo.findById(officeBranch.id()).get();
        assertThat(officeBranchStored.location()).isEqualTo(location);
    }

    @Test
    void itShouldReturnEmptyWhenOfficeBranchIsDeleted() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        officeBranch.delete();
        repo.store(officeBranch);

        var officeBranchStored = repo.findById(officeBranch.id());
        assertThat(officeBranchStored.isEmpty()).isTrue();
    }

    @Test
    void itShouldReturnAllOfficeBranchesRelatedWithOfficeHolder() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeHolder anotherOfficeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        officeHolderRepo.store(anotherOfficeHolder);
        var officeBranch1 = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("image.url"), new Image("image2.url")))
                .withOwner(officeHolder).build();
        var officeBranch2 = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("image4.url")))
                .withOwner(officeHolder).build();
        var officeBranch3 = new OfficeBranchBuilder()
                .withOwner(anotherOfficeHolder).build();
        repo.store(officeBranch1);
        repo.store(officeBranch2);
        repo.store(officeBranch3);

        var officeBranches = repo.findByOfficeHolder(officeHolder);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).flatMap(OfficeBranch::images).containsExactlyInAnyOrder(
                new Image("image.url"),
                new Image("image2.url"),
                new Image("image4.url")
        );
    }

    @Test
    void itShouldReturnAllNonDeletedOfficeBranchesRelatedWithOfficeHolder() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        var officeBranch1 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        officeBranch1.delete();
        var officeBranch2 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        var officeBranch3 = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        repo.store(officeBranch1);
        repo.store(officeBranch2);
        repo.store(officeBranch3);

        var officeBranches = repo.findByOfficeHolder(officeHolder);

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).map(OfficeBranch::toResponse).containsExactlyInAnyOrder(
                officeBranch2.toResponse(),
                officeBranch3.toResponse()
        );
    }

    @Test
    void itShouldUpdateOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder)
                .withImages(List.of(new Image("imageurl"), new Image("image2url")))
                .build();
        repo.store(officeBranch).get();
        var info = OfficeBranchUpdateInformation.of(
                "An awesome updated name",
                null,
                null,
                List.of("imageurl", "image3url"),
                null,
                null,
                null,
                "08122019"
        );

        var officeBranchUpdated = officeBranch.update(info);
        Try<Void> response = repo.update(officeBranchUpdated);

        assertThat(response.isSuccess()).isTrue();
        var officeBranchInDb = repo.findById(officeBranch.id()).get();
        // Updated fields
        assertThat(officeBranchInDb.name()).isEqualTo("An awesome updated name");
        assertThat(officeBranchInDb.location().zipCode()).isEqualTo("08122019");
        assertThat(officeBranchInDb.images()).containsExactlyInAnyOrder(
                new Image("imageurl"),
                new Image("image3url")
        );
        // Fields that were null in the info remains the same
        assertThat(officeBranchInDb.description()).isEqualTo(officeBranch.description());
        assertThat(officeBranchInDb.phone()).isEqualTo(officeBranch.phone());
        assertThat(officeBranchInDb.location().province()).isEqualTo(officeBranch.location().province());
        assertThat(officeBranchInDb.location().city()).isEqualTo(officeBranch.location().city());
        assertThat(officeBranchInDb.location().street()).isEqualTo(officeBranch.location().street());
    }

    @Test
    void itShouldReturnOfficeBranchesThatHaveIdsSpecified() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeHolder anotherOfficeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        officeHolderRepo.store(anotherOfficeHolder);
        var officeBranch1 = new OfficeBranchBuilder().withOwner(officeHolder).build();
        var officeBranch2 = new OfficeBranchBuilder().withOwner(officeHolder).build();
        var officeBranch3 = new OfficeBranchBuilder().withOwner(anotherOfficeHolder).build();
        repo.store(officeBranch1);
        repo.store(officeBranch2);
        repo.store(officeBranch3);

        var officeBranches = repo.findByIds(ImmutableList.of(
                officeBranch1.id(),
                officeBranch3.id()
        ));

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).map(OfficeBranch::toResponse).containsExactlyInAnyOrder(
                officeBranch1.toResponse(),
                officeBranch3.toResponse()
        );
    }

    @Test
    void itShouldReturnNonDeletedOfficeBranchesThatHaveIdsSpecified() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        officeHolderRepo.store(officeHolder);
        var officeBranch1 = new OfficeBranchBuilder().withOwner(officeHolder).build();
        var officeBranch2 = new OfficeBranchBuilder().withOwner(officeHolder).build();
        officeBranch2.delete();
        var officeBranch3 = new OfficeBranchBuilder().withOwner(officeHolder).build();
        repo.store(officeBranch1);
        repo.store(officeBranch2);
        repo.store(officeBranch3);

        var officeBranches = repo.findByIds(ImmutableList.of(
                officeBranch1.id(),
                officeBranch2.id(),
                officeBranch3.id()
        ));

        assertThat(officeBranches).size().isEqualTo(2);
        assertThat(officeBranches).map(OfficeBranch::toResponse).containsExactlyInAnyOrder(
                officeBranch1.toResponse(),
                officeBranch3.toResponse()
        );
    }
}
