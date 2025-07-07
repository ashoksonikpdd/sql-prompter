package com.responsive.ai.sql_prompter.service;

import java.util.Map;

public interface QueryProcessorService {
    Map<String, Object> processQuery(String query);
}
