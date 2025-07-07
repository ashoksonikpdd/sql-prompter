package com.responsive.ai.sql_prompter.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QueryResponse {
    private boolean success;
    private String message;
    private List<Map<String, Object>> data;
    private String generatedQuery;
    private long executionTimeMs;
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGeneratedQuery() {
        return generatedQuery;
    }

    public void setGeneratedQuery(String generatedQuery) {
        this.generatedQuery = generatedQuery;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<Map<String, Object>> getData() {
        if (data == null) {
            return null;
        }
        // Return an unmodifiable view of the list to prevent modification
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    public void setData(List<Map<String, Object>> data) {
        if (data == null) {
            this.data = null;
        } else {
            // Create a new ArrayList to avoid reference to the original list
            this.data = new ArrayList<>(data);
        }
    }
}
