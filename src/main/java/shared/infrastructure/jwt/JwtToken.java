package shared.infrastructure.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
@Getter
public class JwtToken {
    private final String token;
}
