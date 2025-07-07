package com.responsive.ai.sql_prompter;

import com.responsive.ai.sql_prompter.config.PropertySourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Main application class for the SQL Prompter application.
 * This class serves as the entry point for the Spring Boot application.
 */
@SpringBootApplication(exclude = {
    // Exclude YAML auto-configuration
    ConfigurationPropertiesAutoConfiguration.class
})
@Import(PropertySourceConfig.class)
public class SqlPrompterApplication {
    public static void main(String[] args) {
        SpringApplication.run(SqlPrompterApplication.class, args);
    }
}