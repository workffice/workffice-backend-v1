package search.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import search.application.dto.OfficeBranchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "office_branches")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode
public class OfficeBranch {
    @MongoId
    private String       id;
    private String       name;
    private String       phone;
    private String       province;
    private String       city;
    private String       street;
    private List<Office> offices;
    private List<String> images;

    public static OfficeBranch create(
            String id,
            String name,
            String phone,
            String province,
            String city,
            String street,
            List<String> images
    ) {
        return new OfficeBranch(id, name, phone, province, city, street, new ArrayList<>(), images);
    }

    public String id() { return id; }

    public void addNewOffice(Office office) {
        offices.add(office);
    }

    public void removeOffice(String officeId) {
        this.offices = offices
                .stream()
                .filter(office -> !office.id().equals(officeId))
                .collect(Collectors.toList());
    }

    public List<Office> offices() { return offices; }

    public OfficeBranchResponse toResponse() {
        return OfficeBranchResponse.of(
                id,
                name,
                phone,
                province,
                city,
                street,
                offices.stream().map(Office::toResponse).collect(Collectors.toList()),
                images
        );
    }

    public OfficeBranch update(
            String name,
            String phone,
            String province,
            String city,
            String street,
            List<String> images
    ) {
        this.name     = name;
        this.phone    = phone;
        this.province = province;
        this.city     = city;
        this.street   = street;
        this.images   = images;
        return this;
    }
}
