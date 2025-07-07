package com.responsive.ai.sql_prompter.exception;

/**
 * Exception thrown when an invalid or unauthorized query is detected.
 */
public class InvalidQueryException extends RuntimeException {
    
    public InvalidQueryException(String message) {
        super(message);
    }
    
    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
