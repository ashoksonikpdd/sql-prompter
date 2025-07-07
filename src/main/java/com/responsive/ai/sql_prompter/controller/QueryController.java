package com.responsive.ai.sql_prompter.controller;

import com.responsive.ai.sql_prompter.model.QueryRequest;
import com.responsive.ai.sql_prompter.model.QueryResponse;
import com.responsive.ai.sql_prompter.service.QueryService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@RestController
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RequestMapping("/api/query")
@Tag(name = "Query Controller", description = "APIs for executing database queries")
public class QueryController {
    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    private QueryService queryService;
    
    // Rate limiting: 100 requests per minute per IP
    private Bucket bucket;
    
    @PostConstruct
    public void init() {
        // Simple rate limiting: 100 requests per minute
        Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(1));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Operation(
        summary = "Execute a direct SQL query",
        description = "Executes the provided SQL query directly against the database"
    )
    @PostMapping("/direct")
    public ResponseEntity<QueryResponse> executeDirectQuery(@jakarta.validation.Valid @RequestBody QueryRequest request) {
        // Check rate limit
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later.");
        }
        
        log.info("Executing direct query"); // Don't log the actual query for security
        try {
            return ResponseEntity.ok(queryService.executeQuery(request));
        } catch (Exception e) {
            log.error("Error executing direct query", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error executing query: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Execute a natural language query using AI",
        description = "Converts natural language to SQL and executes it against the database"
    )
    @PostMapping("/ai")
    public ResponseEntity<QueryResponse> executeAiQuery(@jakarta.validation.Valid @RequestBody QueryRequest request) {
        // Check rate limit (AI queries consume more resources, so we might want to limit them more)
        if (!bucket.tryConsume(5)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                "Rate limit for AI queries exceeded. Please try again later.");
        }
        
        log.info("Processing AI query"); // Don't log the actual query for security
        request.setUseAi(true);
        
        try {
            return ResponseEntity.ok(queryService.executeQuery(request));
        } catch (Exception e) {
            log.error("Error processing AI query", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Error processing AI query: " + e.getMessage());
        }
    }
}
