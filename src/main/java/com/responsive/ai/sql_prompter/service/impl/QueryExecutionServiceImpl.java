package com.responsive.ai.sql_prompter.service.impl;

import com.responsive.ai.sql_prompter.exception.InvalidQueryException;
import com.responsive.ai.sql_prompter.service.QueryExecutionService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of QueryExecutionService for executing direct MongoDB queries.
 */
@Service
public class QueryExecutionServiceImpl implements QueryExecutionService {
    private static final Logger log = LoggerFactory.getLogger(QueryExecutionServiceImpl.class);
    
    private final MongoTemplate mongoTemplate;

    public QueryExecutionServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Map<String, Object>> executeQuery(String jsonQuery) {
        try {
            // Parse the query to validate it's valid JSON
            Document queryDoc = Document.parse(jsonQuery);
            
            // Extract collection name if present
            String collectionName = queryDoc.getString("collection");
            queryDoc.remove("collection");
            
            // Create the query directly from the document
            BasicQuery query = new BasicQuery(queryDoc);
            
            // Execute the query
            List<Document> documents = collectionName != null ?
                mongoTemplate.find(query, Document.class, collectionName) :
                mongoTemplate.find(query, Document.class);
            
            // Convert List<Document> to List<Map<String, Object>>
            return documents.stream()
                .map(Document::toJson)
                .map(Document::parse)
                .map(doc -> (Map<String, Object>) doc)
                .toList();
            
        } catch (Exception e) {
            log.error("Error executing query: {}", e.getMessage(), e);
            throw new InvalidQueryException("Invalid query: " + e.getMessage(), e);
        }
    }
}
