package backoffice.domain.office_branch;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "locations")
@EqualsAndHashCode(of = {"province", "city", "street", "zipCode"})
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String province;
    @Column
    private String city;
    @Column
    private String street;
    @Column
    private String zipCode;
    
    public Location(String province, String city, String street, String zipCode) {
        this.province = province;
        this.city = city;
        this.street = street;
        this.zipCode = zipCode;
    }

    public Long id() { return id; }

    public String province() { return province; }
    
    public String city() { return city; }
    
    public String street() { return street; }
    
    public String zipCode() { return zipCode;}
}
