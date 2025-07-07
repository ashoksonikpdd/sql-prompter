package com.responsive.ai.sql_prompter.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service interface for database schema operations.
 */
public interface DatabaseSchemaService {
    
    /**
     * Get list of all tables/collections in the database
     * @return List of table/collection names
     */
    Set<String> getTableList();
    
    /**
     * Get schema of a table with sample data
     * @param tableName Name of the table/collection
     * @param sampleSize Number of sample documents to include
     * @return Schema information with sample data
     */
    Map<String, Object> getTableSchemaWithSample(String tableName, int sampleSize);
    
    /**
     * Get schema of a table without sample data
     * @param tableName Name of the table/collection
     * @return Schema information where key is column name and value is column type
     */
    Map<String, String> getTableSchema(String tableName);
    
    /**
     * Get sample data from a table
     * @param tableName Name of the table/collection
     * @param sampleSize Number of sample documents to return
     * @return List of sample documents where each document is represented as a map of field names to values
     */
    List<Map<String, Object>> getTableSample(String tableName, int sampleSize);
    
    /**
     * Get schema as a formatted string
     * @param schemaName Name of the schema/database
     * @return Formatted schema string
     */
    String getSchemaAsString(String schemaName);
    
    /**
     * Get a human-readable schema description
     * @return Schema description as a string
     */
    String getSchemaDescription();
}
