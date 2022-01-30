package search.application.dto;

import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
public class OfficeBranchResponse {
    String               id;
    String               name;
    String               phone;
    String               province;
    String               city;
    String               street;
    List<OfficeResponse> offices;
    List<String>         images;

    @Value(staticConstructor = "of")
    public static class OfficeResponse {
        String  id;
        String  name;
        Integer price;
        Integer capacity;
        Integer tablesQuantity;
        Integer capacityPerTable;
        String  privacy;
    }
}
