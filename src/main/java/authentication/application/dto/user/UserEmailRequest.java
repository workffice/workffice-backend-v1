package authentication.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserEmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String userEmail;
}
