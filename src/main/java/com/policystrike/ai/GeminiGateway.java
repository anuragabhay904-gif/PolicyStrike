
package com.policystrike.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GeminiGateway {

    @Value("${google.api-key:}")
    private String apiKey;

    @Value("${policystrike.gemini.model:gemini-3.5-flash-lite}")
    private String model;

    private volatile Client client;

    private Client getClient() {

        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is not configured. Set GOOGLE_API_KEY."
            );
        }

        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = Client.builder()
                            .apiKey(apiKey)
                            .build();
                }
            }
        }

        return client;
    }

    public String generateText(String prompt) {

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .candidateCount(1)
                        .maxOutputTokens(2048)
                        .build();

        try {

            GenerateContentResponse response =
                    getClient().models.generateContent(
                            model,
                            prompt,
                            config
                    );

            return extractText(response);

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw apiFailure(exception);
        }
    }

    public String generateJson(
            String prompt,
            Map<String, Object> schema) {

        // Convert Java Map into Google's Schema object.
        Schema sdkSchema;

        try {
            sdkSchema = convertSchema(schema);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid Gemini response schema",
                    exception
            );
        }

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .candidateCount(1)
                        .maxOutputTokens(3000)
                        .responseSchema(sdkSchema)
                        .build();

        try {

            GenerateContentResponse response =
                    getClient().models.generateContent(
                            model,
                            prompt,
                            config
                    );

            return extractText(response);

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw apiFailure(exception);
        }
    }

    // Recursively converts nested JSON-schema-style maps
    // into the Google Gen AI SDK Schema representation.
    private Schema convertSchema(Map<?, ?> definition) {

        Object typeValue = definition.get("type");

        if (!(typeValue instanceof String typeName)) {
            throw new IllegalArgumentException(
                    "Schema type is required"
            );
        }

        Type.Known knownType;

        try {
            knownType = Type.Known.valueOf(
                    typeName.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported schema type: " + typeName,
                    exception
            );
        }

        Schema.Builder builder =
                Schema.builder().type(knownType);

        // Handle object properties.
        Object propertiesValue = definition.get("properties");

        if (propertiesValue instanceof Map<?, ?> properties) {

            Map<String, Schema> convertedProperties =
                    new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : properties.entrySet()) {

                if (!(entry.getKey() instanceof String propertyName)
                        || !(entry.getValue() instanceof Map<?, ?> child)) {

                    throw new IllegalArgumentException(
                            "Invalid schema property"
                    );
                }

                convertedProperties.put(
                        propertyName,
                        convertSchema(child)
                );
            }

            builder.properties(convertedProperties);
        }

        // Handle array items.
        Object itemsValue = definition.get("items");

        if (itemsValue instanceof Map<?, ?> items) {
            builder.items(convertSchema(items));
        }

        // Handle required properties.
        Object requiredValue = definition.get("required");

        if (requiredValue instanceof List<?> required) {

            String[] requiredFields = required.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);

            builder.required(requiredFields);
        }

        // Handle enum values.
        Object enumValue = definition.get("enum");

        if (enumValue instanceof List<?> enumValues) {

            String[] allowedValues = enumValues.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);

            builder.enum_(allowedValues);
        }

        return builder.build();
    }

    private String extractText(GenerateContentResponse response) {

        String result = response.text();

        if (result == null || result.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini returned an empty response"
            );
        }

        return result;
    }

    private ResponseStatusException apiFailure(
            RuntimeException exception) {

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Gemini API request failed. Check API key, quota and model configuration.",
                exception
        );
    }
}
