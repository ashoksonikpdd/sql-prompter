package com.responsive.ai.sql_prompter.config;

import com.responsive.ai.sql_prompter.service.AiQueryService;
import com.responsive.ai.sql_prompter.service.DatabaseSchemaService;
import com.responsive.ai.sql_prompter.service.impl.QueryExecutionServiceImpl;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Duration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3}")
    private String ollamaModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing OllamaChatModel with baseUrl: '{}' and model: '{}'", ollamaBaseUrl, ollamaModel);
        try {
            OllamaChatModel model = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModel)
                    .temperature(0.0)
                    .timeout(Duration.ofMinutes(5))
                    .build();
            
            // Test the model with a simple prompt
            String testResponse = model.generate("Say 'Hello, Ollama!'").trim();
            log.info("Ollama test response: {}", testResponse);
            
            return model;
        } catch (Exception e) {
            log.error("Failed to initialize OllamaChatModel. Please verify:", e);
            log.error("1. Ollama service is running at: {}", ollamaBaseUrl);
            log.error("2. Model '{}' is available (check with 'ollama list')", ollamaModel);
            log.error("3. No firewall is blocking the connection");
            throw new RuntimeException("Failed to initialize OllamaChatModel: " + e.getMessage(), e);
        }
    }

    @Bean
    public AiQueryService aiQueryService(MongoTemplate mongoTemplate, 
                                        DatabaseSchemaService schemaService,
                                        ChatLanguageModel chatLanguageModel) {
        return new AiQueryService(mongoTemplate, schemaService, chatLanguageModel);
    }
}
