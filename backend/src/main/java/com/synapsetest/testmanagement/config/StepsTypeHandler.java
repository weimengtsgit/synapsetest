package com.synapsetest.testmanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom MyBatis TypeHandler for test steps
 * Handles steps as a JSON array of objects with {step, action, expected} structure
 * Each element in List<String> is a JSON object string that should be stored as-is
 */
public class StepsTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            // Build a JSON array manually to avoid escaping the JSON object strings
            StringBuilder jsonArray = new StringBuilder("[");
            for (int j = 0; j < parameter.size(); j++) {
                if (j > 0) {
                    jsonArray.append(",");
                }
                String step = parameter.get(j);
                // Check if this element is already a JSON object string
                if (step != null && step.trim().startsWith("{")) {
                    // It's a JSON object, add it as-is without escaping
                    jsonArray.append(step);
                } else {
                    // It's a regular string, escape it properly
                    jsonArray.append(objectMapper.writeValueAsString(step));
                }
            }
            jsonArray.append("]");
            ps.setString(i, jsonArray.toString());
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting steps to JSON", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseStepsList(json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseStepsList(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseStepsList(json);
    }

    private List<String> parseStepsList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // Parse the JSON array and convert each object back to a JSON string
            if (json.trim().startsWith("[") && json.trim().endsWith("]")) {
                // Parse as array of objects, then convert each to string
                List<Object> objects = objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
                List<String> result = new ArrayList<>();
                for (Object obj : objects) {
                    if (obj instanceof String) {
                        result.add((String) obj);
                    } else {
                        // Convert object back to JSON string
                        result.add(objectMapper.writeValueAsString(obj));
                    }
                }
                return result;
            } else {
                // Not an array, treat as single element
                List<String> result = new ArrayList<>();
                result.add(json);
                return result;
            }
        } catch (JsonProcessingException e) {
            // If parsing fails, return list with original json
            List<String> result = new ArrayList<>();
            result.add(json);
            return result;
        }
    }
}
