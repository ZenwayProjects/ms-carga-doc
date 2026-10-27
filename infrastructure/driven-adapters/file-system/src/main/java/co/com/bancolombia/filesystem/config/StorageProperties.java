package co.com.bancolombia.filesystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String basePath = "./uploads";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Path getBasePathAsPath() {
        return Paths.get(basePath);
    }
}
