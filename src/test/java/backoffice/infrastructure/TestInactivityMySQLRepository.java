package backoffice.infrastructure;

import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeRepository;
import backoffice.domain.office_branch.OfficeBranchRepository;
import backoffice.domain.office_holder.OfficeHolderRepository;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityType;
import backoffice.factories.InactivityBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeBuilder;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;
import io.vavr.control.Try;
import server.WorkfficeApplication;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestInactivityMySQLRepository {
    @Autowired
    InactivityMySQLRepo inactivityRepo;
    @Autowired
    OfficeHolderRepository officeHolderRepo;
    @Autowired
    OfficeBranchRepository officeBranchRepo;
    @Autowired
    OfficeRepository officeRepo;

    private Office createOffice() {
        var officeBranch = new OfficeBranchBuilder().build();
        var office = new OfficeBuilder()
                .withOfficeBranch(officeBranch)
                .build();

        officeHolderRepo.store(officeBranch.owner());
        officeBranchRepo.store(officeBranch);
        officeRepo.store(office);
        return office;
    }

    @Test
    void itShouldStoreInactivityWithoutError() {
        var office = createOffice();
        var inactivity = Inactivity.create(
                new InactivityId(),
                InactivityType.RECURRING_DAY,
                Option.of(DayOfWeek.MONDAY),
                Option.none(),
                office
        ).get();
        var inactivity2 = Inactivity.create(
                new InactivityId(),
                InactivityType.SPECIFIC_DATE,
                Option.none(),
                Option.of(LocalDate.now()),
                office
        ).get();

        Try<Void> result = inactivityRepo.store(inactivity);
        Try<Void> result2 = inactivityRepo.store(inactivity2);

        assertThat(result.isFailure()).isFalse();
        assertThat(result2.isFailure()).isFalse();
    }

    @Test
    void itShouldReturnEveryInactivityRelatedWithOffice() {
        var office = createOffice();
        var office2 = createOffice();
        var inactivityId1 = new InactivityId();
        var inactivityId2 = new InactivityId();
        var inactivity = Inactivity.create(
                inactivityId1,
                InactivityType.RECURRING_DAY,
                Option.of(DayOfWeek.MONDAY),
                Option.none(),
                office
        ).get();
        var inactivity2 = Inactivity.create(
                inactivityId2,
                InactivityType.SPECIFIC_DATE,
                Option.none(),
                Option.of(LocalDate.now()),
                office
        ).get();
        var inactivity3 = new Inactivity(
                new InactivityId(),
                InactivityType.SPECIFIC_DATE,
                null,
                LocalDate.now(),
                office2
        );
        inactivityRepo.store(inactivity);
        inactivityRepo.store(inactivity2);
        inactivityRepo.store(inactivity3);

        var inactivities = inactivityRepo.findAllByOffice(office);

        assertThat(inactivities).size().isEqualTo(2);
        assertThat(inactivities).map(Inactivity::id).containsExactlyInAnyOrder(inactivityId1, inactivityId2);
    }

    @Test
    void itShouldDeleteAllInactivitiesRelatedWithOffice() {
        var office = createOffice();
        var inactivity = new InactivityBuilder()
                .withOffice(office)
                .build();
        var inactivity2 = new InactivityBuilder()
                .withOffice(office)
                .build();
        var inactivity3 = new InactivityBuilder()
                .withOffice(office)
                .build();
        inactivityRepo.store(inactivity);
        inactivityRepo.store(inactivity2);
        inactivityRepo.store(inactivity3);

        Try<Void> response = inactivityRepo.delete(ImmutableList.of(inactivity, inactivity2, inactivity3));

        assertThat(response.isSuccess()).isTrue();
        var inactivities = inactivityRepo.findAllByOffice(office);
        assertThat(inactivities).size().isEqualTo(0);
    }

    @Test
    void itShouldStoreAllInactivitiesPassed() {
        var office = createOffice();
        var inactivity = new InactivityBuilder()
                .withOffice(office)
                .build();
        var inactivity2 = new InactivityBuilder()
                .withOffice(office)
                .build();
        var inactivity3 = new InactivityBuilder()
                .withOffice(office)
                .build();

        Try<Void> response = inactivityRepo.bulkStore(ImmutableList.of(inactivity, inactivity2, inactivity3));

        assertThat(response.isSuccess()).isTrue();
        var inactivities = inactivityRepo.findAllByOffice(office);
        assertThat(inactivities).size().isEqualTo(3);
    }
}
