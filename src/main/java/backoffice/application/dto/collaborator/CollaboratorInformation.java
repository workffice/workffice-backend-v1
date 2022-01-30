package backoffice.application.dto.collaborator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class CollaboratorInformation {
    @NotEmpty(message = "Role ids are required")
    private Set<UUID> roleIds;
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    @NotBlank(message = "Name is required")
    private String name;
}
