package backoffice.domain.office_holder;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "office_holders")
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "email"})
public class OfficeHolder {
    
    @EmbeddedId
    private OfficeHolderId id;
    @Column(unique = true)
    private String email;
    
    public OfficeHolder(OfficeHolderId id, String email) {
        this.id = id;
        this.email = email;
    }
    
    public OfficeHolderId id() {
        return id;
    }
    
    public String email() {
        return email;
    }
}
