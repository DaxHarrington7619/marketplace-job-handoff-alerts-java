package com.example.marketplace;

import java.util.Map;

@FunctionalInterface
public interface ErrorReporter {
    void capture(String title, String message, String exception, Map<String, String> context) throws Exception;
}
