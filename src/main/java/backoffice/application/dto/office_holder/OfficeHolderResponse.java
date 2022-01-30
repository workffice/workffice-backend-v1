package backoffice.application.dto.office_holder;

import lombok.Value;

@Value(staticConstructor = "of")
public class OfficeHolderResponse {
    String id;
    String email;
}
