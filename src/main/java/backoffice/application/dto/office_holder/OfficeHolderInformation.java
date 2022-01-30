package backoffice.application.dto.office_holder;

import lombok.Getter;

@Getter
public class OfficeHolderInformation {
    private final String email;
    
    public OfficeHolderInformation(String email) {
        this.email = email;
    }
}
