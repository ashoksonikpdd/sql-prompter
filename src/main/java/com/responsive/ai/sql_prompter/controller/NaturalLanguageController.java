package com.responsive.ai.sql_prompter.controller;

import com.responsive.ai.sql_prompter.model.QueryRequest;
import com.responsive.ai.sql_prompter.model.QueryResponse;
import com.responsive.ai.sql_prompter.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
@Tag(name = "Natural Language Query", description = "APIs for natural language database queries")
public class NaturalLanguageController {
    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageController.class);

    @Autowired
    private QueryService queryService;

    @PostMapping("/nlq")
    @Operation(
        summary = "Process natural language query",
        description = "Process a natural language query and return the results"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Query processed successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = QueryResponse.class)
        )
    )
    public ResponseEntity<QueryResponse> processNaturalLanguageQuery(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Natural language query",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class)
                )
            )
            @RequestBody Map<String, String> request) {
        
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        log.info("Processing natural language query: {}", query);
        
        try {
            QueryRequest queryRequest = new QueryRequest();
            queryRequest.setQuery(query);
            queryRequest.setUseAi(true);
            return ResponseEntity.ok(queryService.executeQuery(queryRequest));
            
        } catch (Exception e) {
            log.error("Error processing query: " + query, e);
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);
        }
    }
}
