package backoffice.application.dto.role;

import lombok.Value;

import java.util.Set;

@Value(staticConstructor = "of")
public class RoleResponse {
    String id;
    String name;
    Set<PermissionResponse> permissions;
    
    @Value(staticConstructor = "of")
    static public class PermissionResponse {
        String resource;
        String access;
    }
}
