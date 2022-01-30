package authentication.application.dto.user;

import lombok.Value;

@Value(staticConstructor = "of")
public class AuthUserResponse {
    String id;
    String email;
    String name;
    String lastname;
    String address;
    String bio;
    String userType;
    String profileImage;
}
