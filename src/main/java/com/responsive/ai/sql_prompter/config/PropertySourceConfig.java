package com.responsive.ai.sql_prompter.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class to explicitly enable only the property sources we need.
 * This disables YAML support and uses only properties files.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    // Exclude YAML auto-configuration and property processing
    ConfigurationPropertiesAutoConfiguration.class,
    PropertyPlaceholderAutoConfiguration.class
})
@Import({
    // Include only necessary property configurations
    PropertyPlaceholderAutoConfiguration.class,
    ProjectInfoAutoConfiguration.class,
    JmxAutoConfiguration.class
})
public class PropertySourceConfig {
    // This class is used to configure property sources
}
