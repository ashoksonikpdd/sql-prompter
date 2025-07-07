package com.responsive.ai.sql_prompter.service;

import com.responsive.ai.sql_prompter.exception.InvalidQueryException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Pattern;


@Service
public final class AiQueryService {
    private static final Logger log = LoggerFactory.getLogger(AiQueryService.class);
    private static final int MAX_RESULT_ROWS = 1000;
    private static final int MAX_QUERY_LENGTH = 10000;
    private static final int MAX_AI_RESPONSE_TIME_MS = 30000; // 30 seconds
    
    private final MongoTemplate mongoTemplate;
    private final DatabaseSchemaService schemaService;
    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructs an AiQueryService with the required dependencies.
     * 
     * @param mongoTemplate The MongoTemplate instance to use for database operations
     * @param schemaService The DatabaseSchemaService for schema-related operations
     * @param chatModel The ChatLanguageModel for AI-based query processing
     */
    public AiQueryService(MongoTemplate mongoTemplate, 
                         DatabaseSchemaService schemaService, 
                         ChatLanguageModel chatModel) {
        // Create defensive copies of mutable objects
        this.mongoTemplate = new MongoTemplate(Objects.requireNonNull(mongoTemplate.getMongoDatabaseFactory()));
        this.schemaService = Objects.requireNonNull(schemaService);
        this.chatModel = Objects.requireNonNull(chatModel);
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing AiQueryService...");
        try {
            // Verify MongoDB connection
            log.info("MongoDB database name: {}", mongoTemplate.getDb().getName());
            
            // List all collections
            Set<String> collections = mongoTemplate.getCollectionNames();
            log.info("Available collections: {}", collections);
            
            // Verify the expected collection exists
            if (!collections.isEmpty()) {
                String firstCollection = collections.iterator().next();
                long count = mongoTemplate.getCollection(firstCollection).countDocuments();
                log.info("Collection '{}' contains {} documents", firstCollection, count);
            }
            
        } catch (Exception e) {
            log.error("Error initializing MongoDB connection: {}", e.getMessage(), e);
        }
    }
    
    // Configuration constants
    
    /**
     * Checks if the input contains potentially dangerous patterns
     * @param input The input string to check
     * @return true if dangerous patterns are found, false otherwise
     */
    private boolean containsInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        // Check for truly dangerous patterns that modify data
        String dangerousPatterns = "(?i)\\b(?:\\.(?:save|insert|update|remove|delete|drop|create|rename|eval|shutdown|fsync|repair|replSet|sharding|logRotate|logRotate|replSetInitiate|replSetReconfig|replSetStepDown|replSetFreeze|replSetMaintenance|replSetSyncFrom|resync|validate|compact|collMod|dbHash|dbStats|cloneCollection|cloneCollectionAsCapped|convertToCapped|createIndexes|dropIndexes|reIndex|filemd5|connPoolSync|connPoolStats|cursorInfo|getCmdLineOpts|getLog|getParameter|getShardVersion|hostInfo|listCommands|listDatabases|ping|profile|serverStatus|shardConnPoolStats|top|whatsmyuri|dbStats|collStats|connPoolStats|cursorInfo|getCmdLineOpts|getLog|getParameter|getShardVersion|hostInfo|listCommands|listDatabases|ping|profile|serverStatus|shardConnPoolStats|top|whatsmyuri|dbStats|collStats|connPoolStats|cursorInfo|getCmdLineOpts|getLog|getParameter|getShardVersion|hostInfo|listCommands|listDatabases|ping|profile|serverStatus|shardConnPoolStats|top|whatsmyuri)\\b|\\$(?:where|function|eval|accumulator|addToSet|push|pull|rename|unset|currentDate|inc|min|max|mul|setOnInsert|pop|bit|isolated|all|elemMatch|size|type|not|mod|regex|text|where|geoWithin|geoIntersects|near|nearSphere|geoNear))\\s*:";
        
        // Only check for dangerous patterns that modify data or access system commands
        return input.matches(".*" + dangerousPatterns + ".*");
    }
    
    /**
     * Adds safety limits to a query document
     */
    private Document addSafetyLimits(Document queryDoc) {
        if (queryDoc == null) {
            return new Document();
        }
        
        // Create a new document to avoid modifying the original
        Document safeQuery = new Document(queryDoc);
        
        // Add a limit to the query if it doesn't have one
        if (!safeQuery.containsKey("$limit") && !safeQuery.containsKey("limit")) {
            // If there's already a $query, add the limit to it
            if (safeQuery.containsKey("$query")) {
                safeQuery.put("$limit", MAX_RESULT_ROWS);
            } else {
                // Otherwise, add it as a top-level limit
                safeQuery.put("limit", MAX_RESULT_ROWS);
            }
        }
        return safeQuery;
    }
    
    // System prompt to guide the AI in generating safe MongoDB queries
    private static final String SYSTEM_PROMPT = "You are an expert MongoDB query generator that converts natural language to precise database queries. " +
            "Your task is to analyze the user's request and generate the most appropriate MongoDB find query.\n\n" +
            "RULES:\n" +
            "1. Always return a valid JSON object with this exact structure: {\"collection\":\"collection_name\", \"query\": {}, \"limit\": 10}\n" +
            "2. The collection name must match exactly with the database collection.\n" +
            "3. The query should be a valid MongoDB find query.\n" +
            "4. Default to case-insensitive regex searches when appropriate.\n" +
            "5. For text searches, use the $text operator when full-text search is needed.\n" +
            "6. Always include a reasonable limit (default to 10).\n" +
            "7. Never include operations that modify data (insert/update/delete/drop/etc).\n" +
            "8. If the request is unclear, make reasonable assumptions and explain in the query.\n\n" +
            "EXAMPLES:\n" +
            "Request: Find all employees named John\n" +
            "Response: {\"collection\":\"employees\",\"query\":{\"name\":{\"$regex\":\"john\",\"$options\":\"i\"}},\"limit\":10}\n\n" +
            "Request: Show me products with price less than 100\n" +
            "Response: {\"collection\":\"products\",\"query\":{\"price\":{\"$lt\":100}},\"limit\":10}\n\n" +
            "Request: Search for documents containing 'urgent' in any field\n" +
            "Response: {\"collection\":\"documents\",\"query\":{\"$text\":{\"$search\":\"urgent\"}},\"limit\":10}";
    
    /**
     * Process a natural language query, convert it to MongoDB query, execute it, and return results
     */
    public List<Map<String, Object>> processAndExecuteQuery(String naturalLanguageQuery) {
        log.info("Starting to process natural language query: {}", naturalLanguageQuery);
        
        if (StringUtils.isBlank(naturalLanguageQuery)) {
            log.error("Empty query provided");
            throw new InvalidQueryException("Query cannot be empty");
        }
        
        if (naturalLanguageQuery.length() > MAX_QUERY_LENGTH) {
            log.error("Query exceeds maximum length: {} > {}", naturalLanguageQuery.length(), MAX_QUERY_LENGTH);
            throw new InvalidQueryException("Query is too long. Maximum length is " + MAX_QUERY_LENGTH + " characters");
        }
        
        // Check for potential injection in natural language query
        if (containsInjection(naturalLanguageQuery)) {
            log.warn("Potential injection attempt detected in query: {}", naturalLanguageQuery);
            throw new SecurityException("Query contains potentially dangerous patterns");
        }
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String jsonQuery = null;
        
        try {
            // 1. Get database schema information
            log.info("Fetching database schema information");
            String schemaInfo = schemaService.getSchemaDescription();
            log.debug("Schema info retrieved. Length: {} characters", schemaInfo.length());
            
            // 2. Generate MongoDB query using AI with timeout
            log.info("Generating MongoDB query from natural language");
            jsonQuery = generateMongoQueryFromNaturalLanguage(naturalLanguageQuery, schemaInfo);
            log.info("Generated query: {}", jsonQuery);
            
            // 3. Validate and parse the generated query
            log.info("Validating and parsing generated query");
            Document queryDoc = validateAndParseQuery(jsonQuery);
            log.debug("Query validated and parsed successfully");
            
            // 4. Add safety limits to the query
            log.debug("Adding safety limits to query");
            queryDoc = addSafetyLimits(queryDoc);
            log.debug("Query with safety limits: {}", queryDoc.toJson());
            
            // 5. Execute the MongoDB query
            log.info("Executing MongoDB query");
            List<Map<String, Object>> results = executeMongoQuery(queryDoc);
            log.info("Query executed successfully. Found {} results", results != null ? results.size() : 0);
            
            return results;
            
        } catch (DataAccessException e) {
            String errorMsg = "Database error: " + e.getMostSpecificCause().getMessage();
            log.error("Database error in AI query processing. Query: {}", jsonQuery, e);
            throw new InvalidQueryException("Error executing query: " + errorMsg, e);
            
        } catch (Exception e) {
            log.error("Error processing AI query. Original query: {}, Generated query: {}", 
                     naturalLanguageQuery, jsonQuery, e);
            throw new RuntimeException("Failed to process AI query: " + e.getMessage(), e);
            
        } finally {
            stopWatch.stop();
            log.info("Query processing completed in {} ms. Original query: {}", 
                   stopWatch.getTotalTimeMillis(), naturalLanguageQuery);
        }
    }
    
    /**
     * Generates a MongoDB query from natural language input using AI
     * @param query The natural language query
     * @param schemaInfo Information about the database schema to assist with query generation
     * @return A valid MongoDB query as a JSON string
     * @throws RuntimeException If there's an error generating or validating the query
     */
    private String generateMongoQueryFromNaturalLanguage(String query, String schemaInfo) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            log.info("Generating MongoDB query for natural language: {}", query);
            
            // Validate input
            if (StringUtils.isBlank(query)) {
                throw new InvalidQueryException("Query cannot be empty or null");
            }
            
            // Pre-process the query to improve understanding
            String processedQuery = preprocessNaturalLanguageQuery(query);
            log.debug("Preprocessed query: {}", processedQuery);
            
            // Check for potential injection in the processed query
            if (containsInjection(processedQuery)) {
                log.warn("Potential injection attempt detected in query: {}", query);
                throw new SecurityException("Query contains potentially dangerous patterns");
            }
            
            // Format the prompt for the AI model
            String prompt = String.format("""
                %s
                
                DATABASE SCHEMA:
                %s
                
                USER REQUEST: %s
                
                INSTRUCTIONS:
                1. Analyze the user's request and determine the most appropriate collection to query
                2. Create a MongoDB find query that matches the user's intent
                3. Use appropriate operators for different types of searches:
                   - For text searches: {"field": {"$regex": "search term", "$options": "i"}}
                   - For exact matches: {"field": "exact value"}
                   - For numeric comparisons: {"field": {"$gt": 100}} or {"field": {"$lt": 100}}
                   - For multiple conditions: {"$and": [{"field1": "value1"}, {"field2": "value2"}]}
                4. Always include a reasonable limit (default to 10)
                5. Never include operations that modify data (insert/update/delete/drop/etc)
                
                RESPONSE FORMAT (must be valid JSON):
                {
                    "collection": "collection_name",
                    "query": {"field": "value"},
                    "limit": 10
                }
                
                Respond with ONLY the JSON object, no additional text or markdown formatting.
                """, 
                SYSTEM_PROMPT, 
                schemaInfo, 
                processedQuery);

            log.debug("Sending prompt to AI model. Prompt length: {} characters", prompt.length());
            
            // Generate the query using the AI model
            log.info("Sending request to AI model");
            String jsonResponse = chatModel.generate(prompt).trim();
            
            if (StringUtils.isBlank(jsonResponse)) {
                throw new InvalidQueryException("AI model did not generate a response");
            }
            
            log.debug("Received response from AI model. Response length: {} characters", jsonResponse.length());
            
            // Extract JSON from the response (might include extra text)
            String jsonQuery = extractJsonFromResponse(jsonResponse);
            log.debug("Extracted JSON query: {}", jsonQuery);
            
            // Validate the JSON structure and content
            validateJsonStructure(jsonQuery);
            
            // Parse and validate the query to ensure it's safe
            Document queryDoc = validateAndParseQuery(jsonQuery);
            
            // Log successful query generation
            log.info("Successfully generated MongoDB query for: {}", query);
            
            return jsonQuery;
            
        } catch (InvalidQueryException e) {
            log.error("Invalid query generated: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to generate a valid query. Please try rephrasing your request. " + 
                                    "Error: " + e.getMessage(), e);
                                    
        } catch (SecurityException e) {
            log.warn("Security violation in query generation: {}", e.getMessage());
            throw new SecurityException("Query contains potentially dangerous patterns. Please modify your request.");
            
        } catch (Exception e) {
            log.error("Unexpected error generating MongoDB query: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while generating the query. Please try again later.", e);
            
        } finally {
            stopWatch.stop();
            log.debug("Query generation completed in {} ms", stopWatch.getTotalTimeMillis());
        }
    }
    
    /**
     * Extracts the first valid JSON object from the model's response.
     * This handles cases where the model returns additional text around the JSON.
     */
    private String extractJsonFromResponse(String response) {
        // First, try to find JSON between ```json and ```
        String jsonPattern = "```(?:json)?\\s*([\\s\\S]*?)\\s*```";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern);
        java.util.regex.Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // If no code block, try to find the first { ... } in the response
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1).trim();
        }
        
        // If no JSON object found, return the original response
        return response.trim();
    }
    
    /**
     * Preprocesses natural language query to improve understanding
     */
    private String preprocessNaturalLanguageQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        // Convert to lowercase for case-insensitive processing
        String processed = query.trim().toLowerCase();
        
        // Remove common filler words that don't affect the query meaning
        processed = processed.replaceAll("\\b(please|can you|could you|i need|i want|show me|find|search for|get|list|display)\\b", "");
        
        // Remove extra whitespace
        processed = processed.replaceAll("\\s+", " ").trim();
        
        // Remove trailing question marks
        processed = processed.replaceAll("\\?$", "");
        
        log.debug("Preprocessed query: '{}' -> '{}'", query, processed);
        return processed;
    }
    
    /**
     * Validates that the JSON structure matches our expected format
     */
    private void validateJsonStructure(String json) throws InvalidQueryException {
        try {
            JsonNode root = objectMapper.readTree(json);
            
            // Check required fields
            if (!root.has("collection") || root.get("collection").asText().trim().isEmpty()) {
                throw new InvalidQueryException("Generated query is missing required 'collection' field");
            }
            
            if (!root.has("query")) {
                throw new InvalidQueryException("Generated query is missing required 'query' field");
            }
            
            // Ensure limit is a positive number
            int limit = root.has("limit") ? root.get("limit").asInt(10) : 10;
            if (limit <= 0) {
                limit = 10;
            } else if (limit > 100) {
                limit = 100; // Cap at 100 results
            }
            
            // Create a new JSON object with the validated limit
            if (root instanceof ObjectNode) {
                ((ObjectNode) root).put("limit", limit);
            }
            
        } catch (JsonProcessingException e) {
            log.error("Invalid JSON structure: {}", json, e);
            throw new InvalidQueryException("Failed to generate a valid query structure. Please try rephrasing your request.");
        } catch (Exception e) {
            log.error("Error processing JSON: {}", e.getMessage(), e);
            throw new InvalidQueryException("Error processing query: " + e.getMessage());
        }
    }
    
    /**
     * Validates and parses a JSON query string into a MongoDB Document
     * @param jsonQuery The JSON string to validate and parse
     * @return A validated MongoDB Document
     * @throws InvalidQueryException If the query is invalid or contains dangerous operations
     */
    private Document validateAndParseQuery(String jsonQuery) throws InvalidQueryException {
        log.debug("Validating and parsing query: {}", jsonQuery);

        if (jsonQuery == null || jsonQuery.trim().isEmpty()) {
            log.error("Received empty JSON query");
            throw new InvalidQueryException("Generated query is empty");
        }

        try {
            // First, parse the JSON to handle date formats properly
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            // Parse the JSON to a Map to handle date conversion
            Map<String, Object> queryMap = mapper.readValue(jsonQuery, new TypeReference<Map<String, Object>>() {});
            
            // Convert the Map to a Document
            Document queryDoc = new Document(queryMap);
            log.trace("Parsed query document: {}", queryDoc.toJson());

            // Check for explicitly dangerous operations
            String dangerousOperators = "(?i)\\$(where|eval|accumulator|function|group|merge|out|geoNear|lookup|graphLookup|redact|$cmd|$query|$orderby|$comment|$hint|$max|$min|$returnKey|$showDiskLoc|$snapshot|$explain|$natural|$meta|$text|$regex|$mod|$type|$all|$elemMatch|$size|$bitsAllClear|$bitsAllSet|$bitsAnyClear|$bitsAnySet|$comment|$meta|$slice)\\b";

            log.debug("Checking for dangerous operators in query");
            // Check for dangerous operators in the query
            for (String key : queryDoc.keySet()) {
                if (key.matches(dangerousOperators)) {
                    throw new SecurityException("Query contains forbidden operator: " + key);
                }

                // Recursively check nested documents
                Object value = queryDoc.get(key);
                if (value instanceof Document) {
                    checkNestedDocument((Document) value, dangerousOperators);
                } else if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        if (item instanceof Document) {
                            checkNestedDocument((Document) item, dangerousOperators);
                        }
                    }
                }
            }

            return queryDoc;

        } catch (JsonProcessingException e) {
            log.error("Error processing JSON query: {}", jsonQuery, e);
            throw new InvalidQueryException("Error processing JSON query: " + e.getMessage());
        } catch (org.bson.json.JsonParseException e) {
            log.error("Invalid JSON format in query: {}", jsonQuery, e);
            throw new InvalidQueryException("Invalid JSON format in query: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error validating/parsing query: {}", e.getMessage(), e);
            throw new InvalidQueryException("Error validating query: " + e.getMessage());
        }
    }
            
    /**
     * Recursively checks a document for dangerous operators
     */
    private void checkNestedDocument(Document doc, String dangerousOperators) {
        for (String key : doc.keySet()) {
            if (key.matches(dangerousOperators)) {
                throw new SecurityException("Query contains forbidden operator in nested document: " + key);
            }
            
            // Recursively check nested documents
            Object value = doc.get(key);
            if (value instanceof Document) {
                checkNestedDocument((Document) value, dangerousOperators);
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Document) {
                        checkNestedDocument((Document) item, dangerousOperators);
                    }
                }
            }
        }
    }
    
    /**
     * Executes a MongoDB query and returns the results as a list of maps
     * @param queryDoc The query document to execute
     * @return List of maps containing the query results
     * @throws InvalidQueryException If there's an error executing the query
     */
    /**
     * Converts a MongoDB Document to a Map, handling nested Documents and Lists
     */
    private Map<String, Object> convertDocumentToMap(Document document) {
        if (document == null) {
            return null;
        }
        
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Document) {
                value = convertDocumentToMap((Document) value);
            } else if (value instanceof List) {
                value = convertList((List<?>) value);
            }
            map.put(entry.getKey(), value);
        }
        return map;
    }
    
    /**
     * Converts a List that might contain Documents to a List with Maps
     */
    private List<Object> convertList(List<?> list) {
        if (list == null) {
            return null;
        }
        
        return list.stream().map(item -> {
            if (item instanceof Document) {
                return convertDocumentToMap((Document) item);
            } else if (item instanceof List) {
                return convertList((List<?>) item);
            } else {
                return item;
            }
        }).collect(Collectors.toList());
    }
    
    /**
     * Executes a MongoDB query and returns the results as a list of maps
     * @param queryDoc The query document to execute
     * @return List of maps containing the query results
     * @throws InvalidQueryException If there's an error executing the query
     */
    private List<Map<String, Object>> executeMongoQuery(Document queryDoc) throws InvalidQueryException {
        log.debug("Executing MongoDB query: {}", queryDoc.toJson());
        
        try {
            // Extract collection name from the query document
            String collectionName = queryDoc.getString("collection");
            if (collectionName == null || collectionName.trim().isEmpty()) {
                throw new InvalidQueryException("No collection specified in the query");
            }
            
            // Get the query criteria, default to empty document if not specified
            Object queryObj = queryDoc.get("query");
            Document criteria;
            if (queryObj == null) {
                criteria = new Document();
            } else if (queryObj instanceof Document) {
                criteria = (Document) queryObj;
            } else if (queryObj instanceof Map) {
                criteria = new Document((Map<String, Object>) queryObj);
            } else {
                throw new InvalidQueryException("Invalid query format. Expected a JSON object.");
            }
            
            // Get the limit, default to 10 if not specified
            int limit = queryDoc.getInteger("limit", 10);
            
            // Create a basic query from the criteria document
            org.springframework.data.mongodb.core.query.BasicQuery query = 
                new org.springframework.data.mongodb.core.query.BasicQuery(criteria.toJson());
            
            // Apply limit
            query.limit(limit);
            
            // Execute the query and convert results
            return mongoTemplate.find(query, Document.class, collectionName)
                .stream()
                .map(this::convertDocumentToMap)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error executing MongoDB query: {}", e.getMessage(), e);
            throw new InvalidQueryException("Error executing query: " + e.getMessage(), e);
        }
    }
}
