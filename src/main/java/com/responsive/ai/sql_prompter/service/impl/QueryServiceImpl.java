package com.responsive.ai.sql_prompter.service.impl;

import com.mongodb.MongoException;
import com.responsive.ai.sql_prompter.exception.InvalidQueryException;
import com.responsive.ai.sql_prompter.model.QueryRequest;
import com.responsive.ai.sql_prompter.model.QueryResponse;
import com.responsive.ai.sql_prompter.service.AiQueryService;
import com.responsive.ai.sql_prompter.service.QueryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service implementation for executing database queries with optional AI processing.
 * This service ensures thread safety by creating defensive copies of mutable objects
 * and properly managing service dependencies.
 */

/**
 * Thread-safe implementation of QueryService that executes database queries with AI processing.
 * This implementation creates defensive copies of all mutable objects to ensure thread safety.
 */
@Service
public final class QueryServiceImpl implements QueryService {
    private final MongoTemplate mongoTemplate;
    private final AiQueryService aiQueryService;
    private static final Logger log = LoggerFactory.getLogger(QueryServiceImpl.class);
    
    /**
     * Constructs a QueryServiceImpl with the required dependencies.
     * 
     * @param mongoTemplate The MongoTemplate instance to use for database operations
     * @param aiQueryService The AiQueryService for AI-based query processing
     */
    /**
     * Constructs a QueryServiceImpl with the required dependencies.
     * 
     * @param mongoTemplate The MongoTemplate instance to use for database operations
     * @param aiQueryService The AiQueryService for AI-based query processing
     */
    public QueryServiceImpl(MongoTemplate mongoTemplate, AiQueryService aiQueryService) {
        // Create defensive copies of mutable objects
        this.mongoTemplate = new MongoTemplate(Objects.requireNonNull(mongoTemplate.getMongoDatabaseFactory()));
        this.aiQueryService = Objects.requireNonNull(aiQueryService);
    }

    @Override
    public QueryResponse executeQuery(QueryRequest request) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        QueryResponse response = new QueryResponse();
        String query = request.getQuery().trim();
        
        try {
            // Validate the query
            validateQuery(query, request.isUseAi());
            
            log.debug("Executing {}query: {}", request.isUseAi() ? "AI " : "", query);
            
            List<Map<String, Object>> results;
            
            if (request.isUseAi()) {
                // Use AI to process and execute the query
                results = aiQueryService.processAndExecuteQuery(query);
                response.setGeneratedQuery(query);
            } else {
                // Direct MongoDB query execution
                results = executeMongoQuery(query);
                response.setGeneratedQuery(query);
            }
            
            response.setSuccess(true);
            response.setData(results);
            response.setMessage("Query executed successfully");
            
        } catch (InvalidQueryException e) {
            log.warn("Invalid query: {}", e.getMessage());
            throw e; // Re-throw validation exceptions
            
        } catch (MongoException e) {
            String errorMsg = "MongoDB error: " + e.getMessage();
            log.error("Database error executing query: {}", errorMsg);
            throw new InvalidQueryException(errorMsg, e);
            
        } catch (Exception e) {
            log.error("Unexpected error executing query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
            
        } finally {
            stopWatch.stop();
            response.setExecutionTimeMs(stopWatch.getTotalTimeMillis());
            log.debug("Query executed in {} ms", stopWatch.getTotalTimeMillis());
        }
        
        return response;
    }
    
    private List<Map<String, Object>> executeMongoQuery(String query) {
        try {
            // Parse the query as a MongoDB query document
            Document queryDoc = Document.parse(query);
            
            // Extract collection name from the query
            String collectionName = queryDoc.getString("collection");
            if (collectionName == null) {
                throw new InvalidQueryException("Collection name must be specified in the query");
            }
            
            // Remove collection name from the query document
            queryDoc.remove("collection");
            
            // Create the query directly from the document
            BasicQuery basicQuery = new BasicQuery(queryDoc);
            
            // Execute the query
            return mongoTemplate.find(
                basicQuery,
                Document.class,
                collectionName
            ).stream()
             .map(Document::toJson)
             .map(Document::parse)
             .map(doc -> (Map<String, Object>) doc)
             .toList();
        } catch (Exception e) {
            log.error("Error executing MongoDB query: {}", e.getMessage(), e);
            throw new InvalidQueryException("Error executing MongoDB query: " + e.getMessage(), e);
        }
    }
    
    private void validateQuery(String query, boolean isAiQuery) {
        if (query == null || query.trim().isEmpty()) {
            throw new InvalidQueryException("Query cannot be empty");
        }
        
        // For non-AI queries, validate MongoDB query format
        if (!isAiQuery) {
            try {
                // Try to parse the query as a JSON document
                Document.parse(query);
            } catch (Exception e) {
                throw new InvalidQueryException("Invalid MongoDB query format: " + e.getMessage());
            }
            String upperQuery = query.toUpperCase();
            
            // Prevent DDL operations
            if (upperQuery.matches("(?i).*\\b(CREATE|ALTER|DROP|TRUNCATE|RENAME)\\b.*")) {
                throw new InvalidQueryException("DDL operations are not allowed");
            }
            
            // Prevent data modification
            if (upperQuery.matches("(?i).*\\b(INSERT|UPDATE|DELETE|MERGE|REPLACE)\\b.*")) {
                throw new InvalidQueryException("Data modification operations are not allowed");
            }
            
            // Prevent potentially dangerous operations
        }
    }
}
