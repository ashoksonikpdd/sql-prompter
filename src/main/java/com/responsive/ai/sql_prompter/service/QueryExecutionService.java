package com.responsive.ai.sql_prompter.service;

import java.util.List;
import java.util.Map;

public interface QueryExecutionService {
    List<Map<String, Object>> executeQuery(String sql);
}
