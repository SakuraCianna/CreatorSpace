package com.creatorspace.module.audit;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSupport() {
    }

    static String toJson(Map<String, ?> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
