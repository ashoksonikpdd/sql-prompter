package com.responsive.ai.sql_prompter.controller;

import com.responsive.ai.sql_prompter.service.DatabaseSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schema")
@Tag(name = "Schema", description = "APIs for database schema information")
public class SchemaController {

    @Autowired
    private DatabaseSchemaService schemaService;

    @GetMapping
    @Operation(summary = "Get database schema", 
               description = "Retrieves the complete database schema with table and column information")
    public ResponseEntity<String> getSchema() {
        return ResponseEntity.ok(schemaService.getSchemaDescription());
    }

    @GetMapping("/tables")
    @Operation(summary = "List all tables", 
               description = "Returns a list of all tables in the database")
    public ResponseEntity<List<String>> listTables() {
        return ResponseEntity.ok(new ArrayList<>(schemaService.getTableList()));
    }

    @GetMapping("/tables/{tableName}")
    @Operation(summary = "Get table schema", 
               description = "Retrieves schema information for a specific table")
    public ResponseEntity<Map<String, ?>> getTableSchema(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "false") boolean includeSample) {
        
        if (includeSample) {
            return ResponseEntity.ok(schemaService.getTableSchemaWithSample(tableName, 5));
        }
        return ResponseEntity.ok(schemaService.getTableSchema(tableName));
    }

    @GetMapping("/tables/{tableName}/sample")
    @Operation(summary = "Get table sample data", 
               description = "Retrieves sample data from a specific table")
    public ResponseEntity<List<Map<String, Object>>> getTableSample(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "10") int limit) {
        
        if (limit <= 0 || limit > 100) {
            limit = 10; // Enforce reasonable limits
        }
        
        return ResponseEntity.ok(schemaService.getTableSample(tableName, limit));
    }
}
