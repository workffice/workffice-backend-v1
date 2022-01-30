package authentication.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@AllArgsConstructor
public class UserLoginInformation {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is not valid")
    private final String email;
    @NotBlank(message = "Password is required")
    private final String password;
}
