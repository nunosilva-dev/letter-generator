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
    /** Fallback diagnostic location for failures without a mapping reference chain. */
    private static final String ROOT_PATH = "document";

    /** Single privately configured mapper, never mutated after publication. */
    private static final ObjectMapper MAPPER = createMapper();

    /** Prevents construction of this static helper. */
    private JsonInput() {}

    /**
     * Deserializes the shared model before the generator's semantic validation pass. JSON null can
     * produce a null document, which that later pass rejects. Unknown fields, duplicate keys,
     * trailing tokens, numeric enums, null primitives and configured scalar coercions fail here.
     *
     * @param json JSON text; Java null is rejected
     * @return deserialized document, or null for the JSON null literal
     * @throws InvalidLetterException if input is absent or cannot be mapped under the strict rules
     */
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

    /**
     * Constructs the only mapper before publication. String length is raised to Integer.MAX_VALUE
     * for embedded image data; other stream constraints retain Jackson defaults. Explicit flags
     * prevent unknown fields, duplicate keys, extra roots and lossy/coercive scalar conversion.
     *
     * @return fully configured mapper published once to readers
     */
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

    /**
     * Converts Jackson's reference chain to dotted fields and bracketed indices. Syntax errors
     * without a mapping chain use document; unknown-property errors append the property when
     * Jackson has not already included its name at the end of the assembled path.
     *
     * @param exception original parsing/mapping failure
     * @return closest field/index path, or document for unlocated syntax errors
     */
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

    /**
     * Adds a field separator only after an existing path, preserving adjacent array indices.
     *
     * @param path diagnostic path under construction
     * @param field field name to append
     */
    private static void appendField(StringBuilder path, String field) {
        if (!path.isEmpty()) {
            path.append('.');
        }
        path.append(field);
    }
}
