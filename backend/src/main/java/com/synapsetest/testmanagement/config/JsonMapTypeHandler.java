package com.synapsetest.testmanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom MyBatis TypeHandler for JSON fields that map to Map<String, String>
 * Converts between Java Map objects and MySQL JSON type
 */
@MappedTypes({Map.class})
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJsonMap(json);
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJsonMap(json);
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJsonMap(json);
    }

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            // Handle empty array case - return empty map
            if (json.trim().equals("[]")) {
                return new HashMap<>();
            }
            // Parse JSON object to Map<String, String>
            if (json.trim().startsWith("{") && json.trim().endsWith("}")) {
                return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            } else {
                // If not a valid object format, return empty map
                return new HashMap<>();
            }
        } catch (JsonProcessingException e) {
            // If parsing fails, return empty map
            return new HashMap<>();
        }
    }
}

