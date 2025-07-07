package com.responsive.ai.sql_prompter.controller;

import com.responsive.ai.sql_prompter.service.DatabaseSeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
public class SeedController {
    
    private final DatabaseSeedService seedService;
    
    public SeedController(DatabaseSeedService seedService) {
        this.seedService = seedService;
    }
    
    @PostMapping
    public ResponseEntity<String> seedDatabase() {
        seedService.seedDatabase();
        return ResponseEntity.ok("Database seeding initiated. Check logs for progress.");
    }
}
