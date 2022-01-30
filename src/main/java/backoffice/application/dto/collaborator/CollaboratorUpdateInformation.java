package backoffice.application.dto.collaborator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class CollaboratorUpdateInformation {

    @NotBlank(message = "Name is required")
    private String name;
    @NotNull(message = "Role ids are required")
    @NotEmpty(message = "Role ids are required")
    private Set<UUID> roleIds;
}
