package backoffice.infrastructure;

import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.domain.equipment.EquipmentRepository;
import backoffice.domain.office.Image;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.Privacy;
import backoffice.domain.office.Tables;
import backoffice.domain.office_branch.OfficeBranch;
import backoffice.domain.office_holder.OfficeHolder;
import backoffice.domain.service.ServiceRepository;
import backoffice.factories.EquipmentBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeBuilder;
import backoffice.factories.OfficeHolderBuilder;
import backoffice.factories.ServiceBuilder;
import com.google.common.collect.ImmutableSet;
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
public class TestOfficeMySQLRepo {
    @Autowired
    OfficeMySQLRepo officeMySQLRepo;
    @Autowired
    OfficeBranchMySQLRepo officeBranchMySQLRepo;
    @Autowired
    OfficeHolderMySQLRepo officeHolderMySQLRepo;
    @Autowired
    ServiceRepository serviceRepo;
    @Autowired
    EquipmentRepository equipmentRepo;

    OfficeBranch createOfficeBranch() {
        OfficeHolder officeHolder = new OfficeHolderBuilder().build();
        OfficeBranch officeBranch = new OfficeBranchBuilder()
                .withOwner(officeHolder).build();
        officeHolderMySQLRepo.store(officeHolder);
        officeBranchMySQLRepo.store(officeBranch);
        return officeBranch;
    }

    @Test
    void itShouldStoreOfficeWithInformationSpecified() {
        var officeBranch = createOfficeBranch();
        OfficeId id = new OfficeId();
        Office office = Office.create(
                id,
                "Some name",
                "Some desc",
                10,
                100,
                new Image("imageUrl"),
                Privacy.SHARED,
                officeBranch,
                Tables.create(10, 10)).get();

        var response = officeMySQLRepo.store(office);

        assertThat(response.isFailure()).isFalse();
        Office officeStored = officeMySQLRepo.findById(id).get();
        assertThat(officeStored.toResponse()).isEqualTo(office.toResponse());
    }

    @Test
    void itShouldStoreIncompleteInformationForTableAndImage() {
        var officeBranch = createOfficeBranch();
        OfficeId id = new OfficeId();
        Office office = Office.create(
                id,
                "Some name",
                "Some desc",
                10,
                100,
                new Image(null),
                Privacy.PRIVATE,
                officeBranch,
                Tables.create(null, null)).get();

        var response = officeMySQLRepo.store(office);

        assertThat(response.isFailure()).isFalse();
        Office officeStored = officeMySQLRepo.findById(id).get();
        assertThat(officeStored.toResponse().getTable())
                .isEqualTo(OfficeResponse.TableResponse.of(null, null));
        assertThat(officeStored.toResponse().getImageUrl()).isEqualTo(null);
    }

    @Test
    void itShouldReturnAllOfficesRelatedWithOfficeBranch() {
        var officeBranch = createOfficeBranch();
        var office1 = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        var office2 = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        var office3 = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        officeMySQLRepo.store(office1);
        officeMySQLRepo.store(office2);
        officeMySQLRepo.store(office3);

        List<Office> offices = officeMySQLRepo.findByOfficeBranch(officeBranch);

        assertThat(offices).size().isEqualTo(3);
    }

    @Test
    void itShouldReturnOfficesRelatedWithOfficeBranchWithServices() {
        var officeBranch = createOfficeBranch();
        var service = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var service2 = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var equipment = new EquipmentBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        serviceRepo.store(service);
        serviceRepo.store(service2);
        equipmentRepo.store(equipment);
        var office1 = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        office1.addServices(ImmutableSet.of(service, service2));
        office1.addEquipments(ImmutableSet.of(equipment));
        officeMySQLRepo.store(office1);

        List<Office> offices = officeMySQLRepo.findByOfficeBranch(officeBranch);

        assertThat(offices).size().isEqualTo(1);
        assertThat(offices.get(0).toResponse().getServices()).containsExactlyInAnyOrder(
                service.toResponse(),
                service2.toResponse()
        );
        assertThat(offices.get(0).toResponse().getEquipments())
                .containsExactly(equipment.toResponse());
    }

    @Test
    void itShouldUpdateOfficeWithSpecifiedInformationInDb() {
        var officeBranch = createOfficeBranch();
        var office = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        officeMySQLRepo.store(office);

        var officeUpdateInfo = OfficeUpdateInformation.of(
                "Some new name",
                null,
                199,
                null,
                null,
                "awesome.image.url",
                null,
                null
        );
        var officeUpdated = office.update(officeUpdateInfo);
        Try<Void> response = officeMySQLRepo.update(officeUpdated.get());

        assertThat(response.isSuccess()).isTrue();
        var officeFromDb = officeMySQLRepo.findById(office.id()).get();
        assertThat(officeFromDb.toResponse().getName()).isEqualTo("Some new name");
        assertThat(officeFromDb.toResponse().getImageUrl()).isEqualTo("awesome.image.url");
        assertThat(officeFromDb.toResponse().getCapacity()).isEqualTo(199);
    }

    @Test
    void itShouldUpdateOfficeWithServiceAndEquipments() {
        var officeBranch = createOfficeBranch();
        var office = new OfficeBuilder().withOfficeBranch(officeBranch).build();
        officeMySQLRepo.store(office);
        var service = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var service2 = new ServiceBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        var equipment = new EquipmentBuilder()
                .withOfficeBranch(officeBranch)
                .build();
        serviceRepo.store(service);
        serviceRepo.store(service2);
        equipmentRepo.store(equipment);

        office.addServices(ImmutableSet.of(service, service2));
        office.addEquipments(ImmutableSet.of(equipment));
        officeMySQLRepo.update(office);

        var officeUpdated = officeMySQLRepo.findById(office.id()).get();
        assertThat(officeUpdated.toResponse().getServices()).containsExactlyInAnyOrder(
                service.toResponse(),
                service2.toResponse()
        );
        assertThat(officeUpdated.toResponse().getEquipments()).containsExactly(equipment.toResponse());
    }
}
