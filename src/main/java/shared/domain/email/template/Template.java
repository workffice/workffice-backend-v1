package shared.domain.email.template;

import java.util.HashMap;

public interface Template {
    String templateName();
    String subject();
    String from();
    HashMap<String, String> substitutionData();
    String plainTextBody();
}
