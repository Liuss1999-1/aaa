package com.example.datadict.provider;

import com.example.datadict.model.DictField;

import java.util.List;
import java.util.Map;

/**
 * Dynamically generates UPDATE statements.
 * Table/column names use ${} (sourced from metadata whitelist),
 * values use #{} (PreparedStatement binding).
 *
 * Uses SYSDATE for Oracle, CURRENT_TIMESTAMP for others.
 */
public class UpdateProvider {

    @SuppressWarnings("unchecked")
    public String update(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<DictField> fields = (List<DictField>) params.get("fieldMappings");
        String dbType = (String) params.getOrDefault("dbType", "mysql");

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        sql.append("ENTRY_NAME = #{entryName}");

        if (isOracle(dbType)) {
            sql.append(", UPDATED_AT = SYSDATE");
        } else {
            sql.append(", UPDATED_AT = CURRENT_TIMESTAMP");
        }

        if (fields != null) {
            for (DictField f : fields) {
                sql.append(", ").append(f.getColumnName()).append(" = #{").append(f.getColumnName()).append("}");
            }
        }

        sql.append(" WHERE ID = #{id}");
        return sql.toString();
    }

    private boolean isOracle(String dbType) {
        return "oracle".equalsIgnoreCase(dbType);
    }
}
