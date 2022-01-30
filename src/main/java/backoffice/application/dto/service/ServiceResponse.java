package backoffice.application.dto.service;

import lombok.Value;

@Value(staticConstructor = "of")
public class ServiceResponse {
    String id;
    String name;
    String category;
}
