package shared.infrastructure.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigUtil {
    
    public static void verifyEnvVariables(Map<String, String> envVariables) throws EnvironmentConfigurationError {
        List<String> missingEnvVariables = envVariables
                .keySet()
                .stream()
                .filter(key -> envVariables.get(key) == null)
                .collect(Collectors.toList());
        if (! missingEnvVariables.isEmpty())
            throw new EnvironmentConfigurationError("Missing env variables: " + missingEnvVariables);
    }
}
