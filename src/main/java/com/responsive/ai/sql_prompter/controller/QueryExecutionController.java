package com.responsive.ai.sql_prompter.controller;

import com.responsive.ai.sql_prompter.service.QueryExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryExecutionController {

    private final QueryExecutionService queryExecutionService;

    @Autowired
    public QueryExecutionController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(
            @RequestParam("query") String query) {
        
        try {
            List<Map<String, Object>> results = queryExecutionService.executeQuery(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            throw new RuntimeException("Error executing query: " + e.getMessage(), e);
        }
    }
}
