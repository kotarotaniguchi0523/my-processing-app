package com.example.agent.service;

import com.example.domain.tools.IAgentTool;

import java.util.List;
import java.util.Map;

/**
 * 既存の詳細な検証ロジックを独立クラスとして抽出したデフォルト実装。
 */
public class DefaultParameterValidator implements ParameterValidator {

    @Override
    public ValidationResult validateParameters(IAgentTool tool, Map<String, Object> parameters) {
        try {
            Map<String, Object> schema = tool.schema();
            if (schema == null) {
                return ValidationResult.valid();
            }
            return validateObject(parameters, schema, "");
        } catch (Exception e) {
            return ValidationResult.invalid("Schema validation error: " + e.getMessage());
        }
    }

    private ValidationResult validateObject(Object value, Map<String, Object> schema, String path) {
        if (value == null) {
            return ValidationResult.valid();
        }
        if (!"object".equals(schema.get("type"))) {
            return validateValue(value, schema, path);
        }
        if (!(value instanceof Map)) {
            return ValidationResult.invalid(buildPath(path) + "Expected object but got " +
                    (value != null ? value.getClass().getSimpleName() : "null"));
        }

        Map<String, Object> objectValue = (Map<String, Object>) value;

        // required fields
        ValidationResult req = validateRequiredFields(objectValue, schema, path);
        if (!req.isValid()) {
            return req;
        }

        Object propertiesObj = schema.get("properties");
        if (!(propertiesObj instanceof Map)) {
            return ValidationResult.valid();
        }
        Map<String, Object> properties = (Map<String, Object>) propertiesObj;

        for (Map.Entry<String, Object> entry : objectValue.entrySet()) {
            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();
            String propertyPath = buildPath(path, propertyName);

            if (properties.containsKey(propertyName)) {
                Map<String, Object> propertySchema = (Map<String, Object>) properties.get(propertyName);
                ValidationResult propertyValidation = validateValue(propertyValue, propertySchema, propertyPath);
                if (!propertyValidation.isValid()) {
                    return propertyValidation;
                }
            } else {
                Object additionalProperties = schema.get("additionalProperties");
                if (Boolean.FALSE.equals(additionalProperties)) {
                    return ValidationResult.invalid(propertyPath + "Additional property '" +
                            propertyName + "' is not allowed");
                }
            }
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateRequiredFields(Map<String, Object> objectValue,
                                                    Map<String, Object> schema, String path) {
        Object requiredObj = schema.get("required");
        if (requiredObj instanceof List) {
            List<String> required = (List<String>) requiredObj;
            for (String field : required) {
                if (!objectValue.containsKey(field)) {
                    return ValidationResult.invalid(buildPath(path) + "Missing required field: " + field);
                }
            }
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateValue(Object value, Map<String, Object> schema, String path) {
        if (value == null) {
            return ValidationResult.valid();
        }
        Object typeObj = schema.get("type");
        if (!(typeObj instanceof String)) {
            return ValidationResult.valid();
        }
        String type = (String) typeObj;
        switch (type) {
            case "string":
                return validateString(value, schema, path);
            case "number":
                return validateNumber(value, schema, path);
            case "integer":
                return validateInteger(value, schema, path);
            case "boolean":
                return validateBoolean(value, path);
            case "array":
                return validateArray(value, schema, path);
            case "object":
                return validateObject(value, schema, path);
            default:
                return ValidationResult.valid();
        }
    }

    private ValidationResult validateString(Object value, Map<String, Object> schema, String path) {
        if (!(value instanceof String)) {
            return ValidationResult.invalid(buildPath(path) + "Expected string but got " +
                    value.getClass().getSimpleName() + " (" + value + ")");
        }
        String stringValue = (String) value;
        Object enumObj = schema.get("enum");
        if (enumObj instanceof List) {
            List<String> enumValues = (List<String>) enumObj;
            if (!enumValues.contains(stringValue)) {
                return ValidationResult.invalid(buildPath(path) + "Value '" + stringValue +
                        "' is not one of allowed values: " + enumValues);
            }
        }
        Object minLenObj = schema.get("minLength");
        if (minLenObj instanceof Number) {
            int min = ((Number) minLenObj).intValue();
            if (stringValue.length() < min) {
                return ValidationResult.invalid(buildPath(path) + "String length " + stringValue.length() +
                        " is less than minimum " + min);
            }
        }
        Object maxLenObj = schema.get("maxLength");
        if (maxLenObj instanceof Number) {
            int max = ((Number) maxLenObj).intValue();
            if (stringValue.length() > max) {
                return ValidationResult.invalid(buildPath(path) + "String length " + stringValue.length() +
                        " exceeds maximum " + max);
            }
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateNumber(Object value, Map<String, Object> schema, String path) {
        if (!(value instanceof Number)) {
            return ValidationResult.invalid(buildPath(path) + "Expected number but got " +
                    value.getClass().getSimpleName() + " (" + value + ")");
        }
        Number numberValue = (Number) value;

        Object minimumObj = schema.get("minimum");
        if (minimumObj instanceof Number) {
            double minimum = ((Number) minimumObj).doubleValue();
            if (numberValue.doubleValue() < minimum) {
                return ValidationResult.invalid(buildPath(path) + "Value " + numberValue +
                        " is less than minimum " + minimum);
            }
        }
        Object maximumObj = schema.get("maximum");
        if (maximumObj instanceof Number) {
            double maximum = ((Number) maximumObj).doubleValue();
            if (numberValue.doubleValue() > maximum) {
                return ValidationResult.invalid(buildPath(path) + "Value " + numberValue +
                        " exceeds maximum " + maximum);
            }
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateInteger(Object value, Map<String, Object> schema, String path) {
        if (!(value instanceof Number)) {
            return ValidationResult.invalid(buildPath(path) + "Expected integer but got " +
                    value.getClass().getSimpleName() + " (" + value + ")");
        }
        Number numberValue = (Number) value;
        if (numberValue instanceof Float || numberValue instanceof Double) {
            double d = numberValue.doubleValue();
            if (d != Math.floor(d)) {
                return ValidationResult.invalid(buildPath(path) + "Expected integer but got floating point number " + d);
            }
        }
        return validateNumber(value, schema, path);
    }

    private ValidationResult validateBoolean(Object value, String path) {
        if (!(value instanceof Boolean)) {
            return ValidationResult.invalid(buildPath(path) + "Expected boolean but got " +
                    value.getClass().getSimpleName() + " (" + value + ")");
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateArray(Object value, Map<String, Object> schema, String path) {
        if (!(value instanceof List)) {
            return ValidationResult.invalid(buildPath(path) + "Expected array but got " +
                    value.getClass().getSimpleName() + " (" + value + ")");
        }
        List<?> arrayValue = (List<?>) value;

        Object minItemsObj = schema.get("minItems");
        if (minItemsObj instanceof Number) {
            int minItems = ((Number) minItemsObj).intValue();
            if (arrayValue.size() < minItems) {
                return ValidationResult.invalid(buildPath(path) + "Array length " + arrayValue.size() +
                        " is less than minimum " + minItems);
            }
        }
        Object maxItemsObj = schema.get("maxItems");
        if (maxItemsObj instanceof Number) {
            int maxItems = ((Number) maxItemsObj).intValue();
            if (arrayValue.size() > maxItems) {
                return ValidationResult.invalid(buildPath(path) + "Array length " + arrayValue.size() +
                        " exceeds maximum " + maxItems);
            }
        }

        Object itemsObj = schema.get("items");
        if (itemsObj instanceof Map) {
            Map<String, Object> itemSchema = (Map<String, Object>) itemsObj;
            for (int i = 0; i < arrayValue.size(); i++) {
                Object item = arrayValue.get(i);
                String itemPath = buildPath(path, "[" + i + "]");
                ValidationResult itemValidation = validateValue(item, itemSchema, itemPath);
                if (!itemValidation.isValid()) {
                    return itemValidation;
                }
            }
        }
        return ValidationResult.valid();
    }

    private String buildPath(String basePath, String property) {
        if (basePath == null || basePath.isEmpty()) {
            return property.startsWith("[") ? property : property;
        }
        return basePath + (property.startsWith("[") ? property : "." + property);
    }

    private String buildPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path + ": ";
    }
}

