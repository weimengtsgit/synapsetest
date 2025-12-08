package com.synapsetest.testmanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom MyBatis TypeHandler for JSON fields
 * Converts between Java objects and MySQL JSON type
 */
@MappedTypes({List.class})
public class JsonTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting object to JSON", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJsonList(json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJsonList(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJsonList(json);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // Check if it's a valid array format
            if (json.trim().startsWith("[") && json.trim().endsWith("]")) {
                // Parse directly to List<String>
                TypeFactory factory = objectMapper.getTypeFactory();
                CollectionType collectionType = factory.constructCollectionType(List.class, String.class);
                return objectMapper.readValue(json, collectionType);
            } else {
                // If not a valid array, treat it as a single string element
                List<String> result = new ArrayList<>();
                result.add(json);
                return result;
            }
        } catch (JsonProcessingException e) {
            // If parsing fails, log the error and return a list with the original json
            List<String> result = new ArrayList<>();
            result.add(json);
            return result;
        }
    }
}

