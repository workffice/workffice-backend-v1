package backoffice.domain.service;

import shared.domain.DomainId;

import java.util.UUID;

public class ServiceId extends DomainId {

    public ServiceId() {
        super();
    }

    public ServiceId(UUID id) {
        super(id);
    }

    public static ServiceId fromString(String id) {
        return new ServiceId(UUID.fromString(id));
    }

}
