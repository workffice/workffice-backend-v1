package backoffice.application.dto.office_branch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class OfficeBranchUpdateInformation {
    private String name, description, phone;
    private List<String> imagesUrls;
    private String province, city, street, zipCode;

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<String> phone() {
        return Optional.ofNullable(phone);
    }

    public Optional<List<String>> imageUrls() {
        return Optional.ofNullable(imagesUrls);
    }

    public Optional<String> province() {
        return Optional.ofNullable(province);
    }

    public Optional<String> city() {
        return Optional.ofNullable(city);
    }

    public Optional<String> street() {
        return Optional.ofNullable(street);
    }

    public Optional<String> zipCode() {
        return Optional.ofNullable(zipCode);
    }
}
