package backoffice.application.dto.role;

import backoffice.domain.role.Access;
import backoffice.domain.role.Resource;
import controller.validators.ValueOfEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class RoleInformation {
    @NotBlank(message = "Role name is required")
    @NotNull(message = "Role name is required")
    String name;
    @NotEmpty(message = "Role Permissions are required")
    List<@Valid Permission> permissions;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Permission {
        @NotNull(message = "Permission Resource is required")
        @NotBlank(message = "Permission Resource is required")
        @ValueOfEnum(enumClass = Resource.class, message = "Permission Resource is invalid")
        String resource;
        @NotNull(message = "Permission Access is required")
        @NotBlank(message = "Permission Access is required")
        @ValueOfEnum(enumClass = Access.class, message = "Permission Access is invalid")
        String access;
    }
}
