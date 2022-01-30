package backoffice.application.dto.office_branch;

import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value(staticConstructor = "of")
public class OfficeBranchResponse {
    String id;
    String name;
    String description;
    String phone;
    LocalDate created;
    List<Image> images;
    Location location;
    
    @Value
    public static class Image {
        String url;
    }
    
    @Value
    public static class Location {
        String province;
        String city;
        String street;
        String zipCode;
    }
}
