package com.responsive.ai.sql_prompter.service.impl;

import com.responsive.ai.sql_prompter.service.DatabaseSchemaService;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of DatabaseSchemaService for MongoDB.
 */
@Service
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public DatabaseSchemaServiceImpl(MongoTemplate mongoTemplate) {
        // Create a new MongoTemplate instance with the same settings to prevent exposure of internal representation
        this.mongoTemplate = new MongoTemplate(
            mongoTemplate.getMongoDatabaseFactory(),
            mongoTemplate.getConverter()
        );
    }

    @Override
    public Set<String> getTableList() {
        return mongoTemplate.getCollectionNames();
    }

    @Override
    public Map<String, Object> getTableSchemaWithSample(String tableName, int sampleSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("tableName", tableName);
        result.put("schema", getTableSchema(tableName));
        result.put("samples", getTableSample(tableName, sampleSize));
        return result;
    }

    @Override
    public Map<String, String> getTableSchema(String tableName) {
        Map<String, String> schema = new HashMap<>();
        List<Document> samples = mongoTemplate.find(
            new org.springframework.data.mongodb.core.query.Query()
                .limit(10), 
            Document.class, 
            tableName
        );

        for (Document doc : samples) {
            collectFields(doc, "", new HashSet<>())
                .forEach(field -> {
                    if (!schema.containsKey(field)) {
                        Object value = getNestedField(doc, field);
                        schema.put(field, getMongoType(value));
                    }
                });
        }
        
        return schema;
    }

    @Override
    public List<Map<String, Object>> getTableSample(String tableName, int sampleSize) {
        List<Map<String, Object>> samples = new ArrayList<>();
        mongoTemplate.find(
            new org.springframework.data.mongodb.core.query.Query()
                .limit(sampleSize),
            Document.class,
            tableName
        ).forEach(doc -> samples.add(doc.entrySet().stream()
            .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll)));
        return samples;
    }

    @Override
    public String getSchemaAsString(String schemaName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Schema: ").append(schemaName).append("\n");
        sb.append("Tables:\n");
        
        for (String table : getTableList()) {
            sb.append("- ").append(table).append("\n");
            Map<String, String> schema = getTableSchema(table);
            for (Map.Entry<String, String> entry : schema.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return sb.toString();
    }

    @Override
    public String getSchemaDescription() {
        return getSchemaAsString("default");
    }

    // Helper methods
    private Set<String> collectFields(Document doc, String prefix, Set<String> fields) {
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String fieldPath = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Document) {
                collectFields((Document) entry.getValue(), fieldPath, fields);
            } else if (entry.getValue() instanceof List) {
                // Handle arrays by checking the first element if it's a document
                List<?> list = (List<?>) entry.getValue();
                if (!list.isEmpty() && list.get(0) instanceof Document) {
                    collectFields((Document) list.get(0), fieldPath + "[]", fields);
                } else {
                    fields.add(fieldPath + "[]");
                }
            } else {
                fields.add(fieldPath);
            }
        }
        return fields;
    }

    private Object getNestedField(Document doc, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Object current = doc;
        
        for (String part : parts) {
            if (part.endsWith("[]")) {
                part = part.substring(0, part.length() - 2);
                if (current instanceof Document) {
                    current = ((Document) current).get(part);
                    if (current instanceof List && !((List<?>) current).isEmpty()) {
                        current = ((List<?>) current).get(0);
                    }
                }
            } else if (current instanceof Document) {
                current = ((Document) current).get(part);
            } else {
                return null;
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }

    private String getMongoType(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "string";
        } else if (value instanceof Number) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof Date) {
            return "date";
        } else if (value instanceof List) {
            return "array";
        } else if (value instanceof Document) {
            return "document";
        } else if (value instanceof ObjectId) {
            return "objectId";
        } else {
            return value.getClass().getSimpleName().toLowerCase();
        }
    }
}
