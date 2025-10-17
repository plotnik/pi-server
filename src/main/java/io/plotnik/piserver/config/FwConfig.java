package io.plotnik.piserver.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "home")
public class FwConfig {
    
    @Bean
    public Path path() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Documents", "pi", "fw");
    }
    
}