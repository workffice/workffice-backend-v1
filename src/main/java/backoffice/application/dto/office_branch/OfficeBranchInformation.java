package backoffice.application.dto.office_branch;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class OfficeBranchInformation {
    String name, description, phone;
    List<String> imagesUrls;
    String province, city, street, zipCode;
    
    public static OfficeBranchInformation of(
            String name,
            String description,
            String phone,
            List<String> imagesUrls,
            String province,
            String city,
            String street,
            String zipCode
    ) {
        return new OfficeBranchInformation(
                name,
                description,
                phone,
                imagesUrls,
                province,
                city,
                street,
                zipCode
        );
    }
}
