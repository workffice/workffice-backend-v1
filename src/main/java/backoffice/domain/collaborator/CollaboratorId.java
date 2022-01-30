package backoffice.domain.collaborator;

import shared.domain.DomainId;

import java.util.UUID;

public class CollaboratorId extends DomainId {

    public CollaboratorId() {
        super();
    }

    public CollaboratorId(UUID id) {
        super(id);
    }

    public static CollaboratorId fromString(String id) {
        return new CollaboratorId(UUID.fromString(id));
    }
}
