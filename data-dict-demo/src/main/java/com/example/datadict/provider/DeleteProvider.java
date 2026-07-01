package com.example.datadict.provider;

import com.example.datadict.model.DictField;

import java.util.List;
import java.util.Map;

/**
 * Dynamically generates DELETE statements.
 * Values use #{} (PreparedStatement binding).
 */
public class DeleteProvider {

    public String deleteById(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        return "DELETE FROM " + tableName + " WHERE ID = #{id}";
    }

    public String deleteByTypeId(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        return "DELETE FROM " + tableName;
    }
}
