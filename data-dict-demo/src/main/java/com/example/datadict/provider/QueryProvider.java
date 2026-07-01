package com.example.datadict.provider;

import com.example.datadict.model.DictField;

import java.util.List;
import java.util.Map;

/**
 * Dynamically generates SELECT statements with tag intersection filtering and pagination.
 * Oracle: OFFSET ... ROWS FETCH NEXT ... ROWS ONLY
 * Others (H2/MySQL): LIMIT ... OFFSET ...
 *
 * Table/column names use ${} (sourced from metadata whitelist),
 * all parameters use #{} (PreparedStatement binding).
 */
public class QueryProvider {

    @SuppressWarnings("unchecked")
    public String findById(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<DictField> fields = (List<DictField>) params.get("fieldMappings");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ID, ENTRY_NAME");
        appendFieldColumns(sql, fields);
        sql.append(", CREATED_AT, UPDATED_AT");
        sql.append(" FROM ").append(tableName);
        sql.append(" WHERE ID = #{id}");
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    public String queryPage(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<DictField> fields = (List<DictField>) params.get("fieldMappings");
        List<String> tagIds = (List<String>) params.get("tagIds");
        String dbType = (String) params.getOrDefault("dbType", "mysql");

        StringBuilder sql = new StringBuilder();

        // Oracle: wrap with pagination subquery
        boolean oracle = "oracle".equalsIgnoreCase(dbType);
        if (oracle) {
            sql.append("SELECT * FROM (SELECT t.*, ROWNUM rn FROM (");
        }

        sql.append("SELECT e.ID, e.ENTRY_NAME");
        appendFieldColumnsAlias(sql, fields, "e");
        sql.append(", e.CREATED_AT, e.UPDATED_AT");
        sql.append(" FROM ").append(tableName).append(" e");

        if (tagIds != null && !tagIds.isEmpty()) {
            for (int i = 0; i < tagIds.size(); i++) {
                String alias = "tg" + i;
                sql.append(" INNER JOIN DICT_ENTRY_TAG ").append(alias);
                sql.append(" ON ").append(alias).append(".TYPE_ID = #{typeId}");
                sql.append(" AND ").append(alias).append(".ENTRY_ID = e.ID");
                sql.append(" AND ").append(alias).append(".TAG_ID = #{tagId").append(i).append("}");
            }
        }

        sql.append(" ORDER BY e.CREATED_AT DESC");

        if (oracle) {
            // Oracle pagination with ROWNUM (compatible with old Oracle versions)
            sql.append(") t) WHERE rn > #{offset} AND rn <= #{endRow}");
        } else {
            sql.append(" LIMIT #{limit} OFFSET #{offset}");
        }
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    public String countPage(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<String> tagIds = (List<String>) params.get("tagIds");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(tableName).append(" e");

        if (tagIds != null && !tagIds.isEmpty()) {
            for (int i = 0; i < tagIds.size(); i++) {
                String alias = "tg" + i;
                sql.append(" INNER JOIN DICT_ENTRY_TAG ").append(alias);
                sql.append(" ON ").append(alias).append(".TYPE_ID = #{typeId}");
                sql.append(" AND ").append(alias).append(".ENTRY_ID = e.ID");
                sql.append(" AND ").append(alias).append(".TAG_ID = #{tagId").append(i).append("}");
            }
        }

        return sql.toString();
    }

    private void appendFieldColumns(StringBuilder sql, List<DictField> fields) {
        if (fields != null) {
            for (DictField f : fields) {
                sql.append(", ").append(f.getColumnName());
            }
        }
    }

    private void appendFieldColumnsAlias(StringBuilder sql, List<DictField> fields, String alias) {
        if (fields != null) {
            for (DictField f : fields) {
                sql.append(", ").append(alias).append(".").append(f.getColumnName())
                   .append(" AS ").append(f.getColumnName());
            }
        }
    }
}
