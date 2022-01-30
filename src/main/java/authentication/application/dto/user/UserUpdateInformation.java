package authentication.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class UserUpdateInformation {
    private String name;
    private String lastname;
    private String address;
    private String bio;
    private String profileImage;
}
