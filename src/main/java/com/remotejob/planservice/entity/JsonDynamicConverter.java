package com.remotejob.planservice.entity;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A JPA attribute converter that converts between a JsonNode object and its
 * String representation for storing in a database column.
 * <p>
 * This converter leverages the Jackson library to handle the JSON parsing and
 * serialization. It is intended for automatic application where a JsonNode type
 * is used as an entity attribute and needs to be stored as a JSON string in
 * the database.
 * <p>
 * The @Converter(autoApply = true) annotation indicates that this converter
 * should be automatically applied to all mapped attributes of the specified
 * type (JsonNode).
 */
@Converter(autoApply = true)
class JsonDynamicConverter implements AttributeConverter<JsonNode, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute.asText();
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(dbData);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON string from database", e);
        }
    }
}