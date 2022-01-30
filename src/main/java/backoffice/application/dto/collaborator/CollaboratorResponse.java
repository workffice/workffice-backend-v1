package backoffice.application.dto.collaborator;

import lombok.Value;

import java.time.LocalDate;

@Value(staticConstructor = "of")
public class CollaboratorResponse {
    String    id;
    String    email;
    String    name;
    String    status;
    LocalDate created;
}
