package backoffice.domain;

import backoffice.application.dto.office.OfficeResponse;
import backoffice.application.dto.office.OfficeUpdateInformation;
import backoffice.domain.office.Image;
import backoffice.domain.office.Office;
import backoffice.domain.office.OfficeId;
import backoffice.domain.office.Privacy;
import backoffice.domain.office.Tables;
import backoffice.factories.EquipmentBuilder;
import backoffice.factories.OfficeBranchBuilder;
import backoffice.factories.OfficeBuilder;
import backoffice.factories.ServiceBuilder;
import com.google.common.collect.ImmutableSet;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOffice {
    @Test
    void itShouldReturnFailureWhenOfficeIsPrivateAndTablePropertiesAreNotSpecified() {
        var office = Office.create(
                new OfficeId(),
                "monumental",
                "some description",
                10,
                100,
                new Image("imageurl"),
                Privacy.SHARED,
                new OfficeBranchBuilder().build(),
                Tables.create(10, null)
        );

        assertThat(office.isFailure()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void itShouldStoreEmptyImageWhenImageInformationIsEmpty(String url) {
        var office = Office.create(
                new OfficeId(),
                "monumental",
                "some description",
                10,
                100,
                new Image(url),
                Privacy.SHARED,
                new OfficeBranchBuilder().build(),
                Tables.create(10, 10)
        ).get();

        assertThat(office.toResponse().getImageUrl()).isEqualTo(null);
    }

    @Test
    void itShouldStoreEmptyTableWhenTableInformationIsEmpty() {
        var office = Office.create(
                new OfficeId(),
                "monumental",
                "some description",
                10,
                100,
                new Image("imageurl"),
                Privacy.PRIVATE,
                new OfficeBranchBuilder().build(),
                Tables.create(null, null)
        ).get();

        assertThat(office.toResponse().getTable()).isEqualTo(OfficeResponse.TableResponse.of(null, null));
    }

    @Test
    void itShouldReturnFailureWhenTryingToUpdateOfficeToSharedWithoutTables() {
        var office = new OfficeBuilder().build();
        var officeUpdateInfo = OfficeUpdateInformation.of(
                null,
                null,
                null,
                null,
                Privacy.SHARED.name(),
                null,
                null,
                null
        );

        var officeUpdateOrError = office.update(officeUpdateInfo);

        assertThat(officeUpdateOrError.isFailure()).isTrue();
        assertThat(officeUpdateOrError.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itShouldUpdateFieldsSpecified() {
        var office = new OfficeBuilder()
                .withName("First name")
                .withDescription("First desc")
                .withCapacity(1)
                .withPrice(100)
                .withImage(new Image("imageurl.com"))
                .withPrivacy(Privacy.SHARED)
                .withTables(Tables.create(10, 10))
                .build();
        var officeUpdateInfo = OfficeUpdateInformation.of(
                "Updated name",
                "Second desc",
                100,
                1000,
                Privacy.PRIVATE.name(),
                "newimage.url",
                2,
                5
        );

        var officeUpdate = office.update(officeUpdateInfo).get();

        var officeResponse = officeUpdate.toResponse();
        assertThat(officeResponse.getName()).isEqualTo("Updated name");
        assertThat(officeResponse.getDescription()).isEqualTo("Second desc");
        assertThat(officeResponse.getCapacity()).isEqualTo(100);
        assertThat(officeResponse.getPrice()).isEqualTo(1000);
        assertThat(officeResponse.getImageUrl()).isEqualTo("newimage.url");
        assertThat(officeResponse.getPrivacy()).isEqualTo(Privacy.PRIVATE.name());
        assertThat(officeResponse.getTable()).isEqualTo(OfficeResponse.TableResponse.of(2, 5));
    }

    @Test
    void itShouldKeepTheSameValueWhenFieldsAreNotSpecified() {
        var office = new OfficeBuilder()
                .withName("First name")
                .withDescription("First desc")
                .withCapacity(1)
                .withPrice(100)
                .withImage(new Image("imageurl.com"))
                .withPrivacy(Privacy.SHARED)
                .withTables(Tables.create(10, 10))
                .build();
        var officeUpdateInfo = OfficeUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeUpdate = office.update(officeUpdateInfo).get();

        var officeResponse = officeUpdate.toResponse();
        assertThat(officeResponse.getName()).isEqualTo("First name");
        assertThat(officeResponse.getDescription()).isEqualTo("First desc");
        assertThat(officeResponse.getCapacity()).isEqualTo(1);
        assertThat(officeResponse.getPrice()).isEqualTo(100);
        assertThat(officeResponse.getImageUrl()).isEqualTo("imageurl.com");
        assertThat(officeResponse.getPrivacy()).isEqualTo(Privacy.SHARED.name());
        assertThat(officeResponse.getTable()).isEqualTo(OfficeResponse.TableResponse.of(10, 10));
    }

    @Test
    void itShouldReturnFalseWhenOfficeIsNotDeleted() {
        var office = new OfficeBuilder().build();

        assertThat(office.isDeleted(LocalDate.now(Clock.systemUTC()))).isFalse();
    }

    @Test
    void itShouldReturnFalseWhenOfficeHasADeletedDateButIsInTheFuture() {
        var office = new OfficeBuilder().build();
        office.delete(LocalDate.now(Clock.systemUTC()).plusDays(10));

        assertThat(office.isDeleted(LocalDate.now(Clock.systemUTC()))).isFalse();
    }

    @Test
    void itShouldReturnTrueWhenOfficeDeletedDayAlreadyHappened() {
        var office = new OfficeBuilder().build();
        office.delete(LocalDate.now(Clock.systemUTC()).minusDays(1));

        assertThat(office.isDeleted(LocalDate.now(Clock.systemUTC()))).isTrue();
    }

    @Test
    void itShouldNotDuplicateServicesAndEquipments() {
        var office = new OfficeBuilder().build();
        var service1 = new ServiceBuilder().build();
        var service2 = new ServiceBuilder().build();
        var equipment1 = new EquipmentBuilder().build();
        var equipment2 = new EquipmentBuilder().build();

        office.addServices(ImmutableSet.of(service1, service2));
        office.addServices(ImmutableSet.of(service1, service2));
        office.addEquipments(ImmutableSet.of(equipment1));
        office.addEquipments(ImmutableSet.of(equipment2, equipment1));

        assertThat(office.toResponse().getServices()).containsExactlyInAnyOrder(
                service1.toResponse(),
                service2.toResponse()
        );
        assertThat(office.toResponse().getEquipments()).containsExactlyInAnyOrder(
                equipment1.toResponse(),
                equipment2.toResponse()
        );
    }
}
