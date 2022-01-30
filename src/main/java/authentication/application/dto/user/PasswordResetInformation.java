package authentication.application.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
public class PasswordResetInformation {
    @NotBlank(message = "Password is required")
    private String password;
}
