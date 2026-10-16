package com.nsdev.lettergenerator.internal.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;

/** Strict JSON boundary; the configured mapper is never exposed or mutated after initialization. */
public final class JsonInput {
    private static final String ROOT_PATH = "document";
    private static final ObjectMapper MAPPER = createMapper();

    private JsonInput() {}

    public static LetterDocument read(String json) {
        if (json == null) {
            throw new InvalidLetterException(ROOT_PATH, "JSON is required");
        }
        try {
            return MAPPER.readValue(json, LetterDocument.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidLetterException(
                    fieldPath(exception), exception.getOriginalMessage(), exception);
        }
    }

    private static ObjectMapper createMapper() {
        JsonFactory factory =
                JsonFactory.builder()
                        .streamReadConstraints(
                                StreamReadConstraints.builder()
                                        .maxStringLength(Integer.MAX_VALUE)
                                        .build())
                        .build();
        return JsonMapper.builder(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
    }

    private static String fieldPath(JsonProcessingException exception) {
        if (!(exception instanceof JsonMappingException mapping)) {
            return ROOT_PATH;
        }
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : mapping.getPath()) {
            if (reference.getFieldName() != null) {
                appendField(path, reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        if (mapping instanceof UnrecognizedPropertyException unknown
                && !path.toString().endsWith(unknown.getPropertyName())) {
            appendField(path, unknown.getPropertyName());
        }
        return path.isEmpty() ? ROOT_PATH : path.toString();
    }

    private static void appendField(StringBuilder path, String field) {
        if (!path.isEmpty()) {
            path.append('.');
        }
        path.append(field);
    }
}
