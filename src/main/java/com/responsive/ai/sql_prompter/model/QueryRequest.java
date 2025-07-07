package com.responsive.ai.sql_prompter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Represents a query request with validation annotations and improved documentation.
 */
public class QueryRequest {
    private String query;
    private boolean useAi = false;
    /**
     * The SQL query to be executed or natural language query to be processed.
     * Must be between 1 and 5000 characters long and cannot be blank.
     */
    /**
     * The SQL query to be executed or natural language query to be processed.
     * Must be between 1 and 5000 characters long and cannot be blank.
     */
    @NotBlank(message = "Query cannot be empty")
    @Size(min = 1, max = 5000, message = "Query must be between 1 and 5000 characters")
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Flag indicating whether to use AI for query processing.
     * If true, the query will be treated as natural language and converted to SQL.
     */
    public boolean isUseAi() {
        return useAi;
    }

    public void setUseAi(boolean useAi) {
        this.useAi = useAi;
    }
}
