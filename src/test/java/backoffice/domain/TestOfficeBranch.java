package backoffice.domain;

import backoffice.application.dto.office_branch.OfficeBranchUpdateInformation;
import backoffice.domain.office_branch.Image;
import backoffice.domain.office_branch.Location;
import backoffice.factories.OfficeBranchBuilder;
import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOfficeBranch {

    @Test
    void itShouldUpdateNameWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                "New spectacular name",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.name()).isEqualTo("New spectacular name");
    }

    @Test
    void itShouldKeepTheSameNameWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withName("MMM")
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.name()).isEqualTo("MMM");
    }

    @Test
    void itShouldUpdateDescriptionWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                "Some new desc",
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.description()).isEqualTo("Some new desc");
    }

    @Test
    void itShouldKeepTheSameDescriptionWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withDescription("First desc")
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.description()).isEqualTo("First desc");
    }

    @Test
    void itShouldUpdatePhoneWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                "123456",
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.phone()).isEqualTo("123456");
    }

    @Test
    void itShouldKeepTheSamePhoneWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withPhone("1")
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.phone()).isEqualTo("1");
    }

    @Test
    void itShouldUpdateProvinceWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                "Buenos Aires",
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().province()).isEqualTo("Buenos Aires");
    }

    @Test
    void itShouldKeepTheSameProvinceWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withLocation(new Location("Mendoza", "a", "a", "1"))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().province()).isEqualTo("Mendoza");
    }

    @Test
    void itShouldUpdateCityWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                "Belgrano",
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().city()).isEqualTo("Belgrano");
    }

    @Test
    void itShouldKeepTheSameCityWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withLocation(new Location("Mendoza", "Godoy Cruz", "a", "1"))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().city()).isEqualTo("Godoy Cruz");
    }

    @Test
    void itShouldUpdateStreetWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder().build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                "Volcan Santa Maria",
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().street()).isEqualTo("Volcan Santa Maria");
    }

    @Test
    void itShouldKeepTheSameStreetWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withLocation(new Location("Mendoza", "a", "Gab", "1"))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().street()).isEqualTo("Gab");
    }

    @Test
    void itShouldUpdateZipCodeWhenItIsProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withLocation(new Location("Mendoza", "a", "a", "1"))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "5501"
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().zipCode()).isEqualTo("5501");
    }

    @Test
    void itShouldKeepTheSameZipCodeWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withLocation(new Location("Mendoza", "a", "a", "1"))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.location().zipCode()).isEqualTo("1");
    }

    @Test
    void itShouldKeepImagesWithSameUrl() {
        var officeBranch = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("image.com"), new Image("image3.com")))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                ImmutableList.of("image.com", "image2.com"),
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.images()).containsExactlyInAnyOrder(
                new Image("image.com"),
                new Image("image2.com")
        );
    }

    @Test
    void itShouldReplaceAllImagesWhenAreAllNew() {
        var officeBranch = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("image3.com")))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                ImmutableList.of("image.com", "image2.com"),
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.images()).containsExactlyInAnyOrder(
                new Image("image.com"),
                new Image("image2.com")
        );
    }

    @Test
    void itShouldKeepTheSameImagesWhenItIsNotProvided() {
        var officeBranch = new OfficeBranchBuilder()
                .withImages(ImmutableList.of(new Image("image.com"), new Image("image2.com")))
                .build();
        var info = OfficeBranchUpdateInformation.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var officeBranchUpdated = officeBranch.update(info);

        assertThat(officeBranchUpdated.images()).containsExactlyInAnyOrder(
                new Image("image.com"),
                new Image("image2.com")
        );
    }
}
