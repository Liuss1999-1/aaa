package com.example.datadict.provider;

import com.example.datadict.model.DictField;

import java.util.List;
import java.util.Map;

/**
 * Dynamically generates INSERT statements.
 * Table/column names use ${} (sourced from metadata whitelist — safe),
 * values use #{} (PreparedStatement parameter binding — injection proof).
 *
 * Detects Oracle vs H2/MySQL via "dbType" param — uses SYSDATE for Oracle,
 * CURRENT_TIMESTAMP for others.
 */
public class InsertProvider {

    @SuppressWarnings("unchecked")
    public String insert(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<DictField> fields = (List<DictField>) params.get("fieldMappings");
        String dbType = (String) params.getOrDefault("dbType", "mysql");

        String now = isOracle(dbType) ? "SYSDATE" : "CURRENT_TIMESTAMP";

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (ID, ENTRY_NAME");

        if (fields != null) {
            for (DictField f : fields) {
                sql.append(", ").append(f.getColumnName());
            }
        }

        sql.append(", CREATED_AT, UPDATED_AT) VALUES (");
        sql.append("#{id}, #{entryName}");

        if (fields != null) {
            for (DictField f : fields) {
                sql.append(", #{").append(f.getColumnName()).append("}");
            }
        }

        sql.append(", ").append(now).append(", ").append(now).append(")");
        return sql.toString();
    }

    private boolean isOracle(String dbType) {
        return "oracle".equalsIgnoreCase(dbType);
    }
}
