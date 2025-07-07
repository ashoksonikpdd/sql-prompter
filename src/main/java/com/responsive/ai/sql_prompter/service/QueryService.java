package com.responsive.ai.sql_prompter.service;

import com.responsive.ai.sql_prompter.model.QueryRequest;
import com.responsive.ai.sql_prompter.model.QueryResponse;

public interface QueryService {
    QueryResponse executeQuery(QueryRequest request);
}
