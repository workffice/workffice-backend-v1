package backoffice.domain.collaborator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(staticName = "of")
@Getter
public class CollaboratorToken {
    private final String token;
}
