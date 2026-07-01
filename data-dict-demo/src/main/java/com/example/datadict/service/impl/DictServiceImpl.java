package com.example.datadict.service.impl;

import com.example.datadict.exception.BusinessException;
import com.example.datadict.mapper.DictDynamicMapper;
import com.example.datadict.mapper.DictMetadataMapper;
import com.example.datadict.mapper.DictTagMapper;
import com.example.datadict.model.*;
import com.example.datadict.service.DictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Metadata-driven dictionary service implementation.
 *
 * Core idea: DICT_TYPE tells us which physical table to use;
 * DICT_FIELD tells us the column mappings (fieldCode -> columnName).
 * CRUD operations look up this metadata at runtime and dynamically
 * build SQL via MyBatis Provider classes.
 *
 * Tag filtering uses multiple INNER JOINs on DICT_ENTRY_TAG for intersection semantics.
 */
@Service
public class DictServiceImpl implements DictService {

    /** Safe-name pattern: uppercase start, alphanumeric + underscore, max 64 chars */
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

    /** Maximum page size to prevent DoS */
    private static final int MAX_PAGE_SIZE = 200;

    /** Accessor for detecting Oracle in SQL Providers */
    @org.springframework.beans.factory.annotation.Value("${spring.datasource.url:}")
    private String datasourceUrl;

    private final DictMetadataMapper metadataMapper;
    private final DictDynamicMapper dynamicMapper;
    private final DictTagMapper tagMapper;

    public DictServiceImpl(DictMetadataMapper metadataMapper, DictDynamicMapper dynamicMapper, DictTagMapper tagMapper) {
        this.metadataMapper = metadataMapper;
        this.dynamicMapper = dynamicMapper;
        this.tagMapper = tagMapper;
    }

    // ==================== Type Management ====================

    @Override
    @Transactional
    public DictType createType(TypeSaveRequest req) {
        DictType existing = metadataMapper.findByTypeCode(req.getTypeCode());
        if (existing != null) {
            throw BusinessException.conflict("Type code already exists: " + req.getTypeCode());
        }
        validateSafeName(req.getTableName(), "tableName");
        DictType type = new DictType();
        type.setId(UUID.randomUUID().toString().replace("-", ""));
        type.setTypeCode(req.getTypeCode());
        type.setTypeName(req.getTypeName());
        type.setTableName(req.getTableName());
        metadataMapper.insertType(type);
        return type;
    }

    @Override
    @Transactional
    public DictType updateType(TypeSaveRequest req) {
        DictType type = requireType(req.getTypeCode());
        type.setTypeName(req.getTypeName());
        validateSafeName(req.getTableName(), "tableName");
        type.setTableName(req.getTableName());
        metadataMapper.updateType(type);
        return type;
    }

    @Override
    @Transactional
    public void deleteType(String typeCode) {
        DictType type = requireType(typeCode);

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", type.getTableName());
        dynamicMapper.deleteAllByTable(params);

        tagMapper.deleteByTypeId(type.getId());
        metadataMapper.deleteFieldsByTypeId(type.getId());
        metadataMapper.deleteType(typeCode);
    }

    @Override
    public TypeDetail getType(String typeCode) {
        DictType type = requireType(typeCode);
        List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId());
        return new TypeDetail(type, fields);
    }

    @Override
    public List<DictType> listTypes() {
        return metadataMapper.listTypes();
    }

    // ==================== Field Management ====================

    @Override
    @Transactional
    public DictField addField(String typeCode, FieldSaveRequest req) {
        DictType type = requireType(typeCode);
        DictField existing = metadataMapper.findField(type.getId(), req.getFieldCode());
        if (existing != null) {
            throw BusinessException.conflict("Field code already exists: " + req.getFieldCode());
        }
        validateSafeName(req.getColumnName(), "columnName");
        DictField field = new DictField();
        field.setId(UUID.randomUUID().toString().replace("-", ""));
        field.setTypeId(type.getId());
        field.setFieldCode(req.getFieldCode());
        field.setFieldName(req.getFieldName());
        field.setFieldType(req.getFieldType());
        field.setColumnName(req.getColumnName());
        field.setSortOrder(req.getSortOrder());
        field.setIsRequired(req.getIsRequired());
        metadataMapper.insertField(field);
        return field;
    }

    @Override
    @Transactional
    public DictField updateField(String typeCode, FieldSaveRequest req) {
        DictType type = requireType(typeCode);
        DictField field = metadataMapper.findField(type.getId(), req.getFieldCode());
        if (field == null) {
            throw BusinessException.notFound("Field", req.getFieldCode());
        }
        validateSafeName(req.getColumnName(), "columnName");
        field.setFieldName(req.getFieldName());
        field.setFieldType(req.getFieldType());
        field.setColumnName(req.getColumnName());
        field.setSortOrder(req.getSortOrder());
        field.setIsRequired(req.getIsRequired());
        metadataMapper.updateField(field);
        return field;
    }

    @Override
    @Transactional
    public void deleteField(String typeCode, String fieldCode) {
        DictType type = requireType(typeCode);
        metadataMapper.deleteField(type.getId(), fieldCode);
    }

    // ==================== Entry CRUD ====================

    @Override
    @Transactional
    public DictEntry createEntry(EntrySaveRequest req) {
        DictType type = requireType(req.getTypeCode());
        List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId());

        validateFields(req.getFields(), fields, true);
        validateFieldTypes(req.getFields(), fields);

        String id = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> params = buildDataParams(type.getTableName(), id, req.getEntryName(), fields, req.getFields());
        params.put("fieldMappings", fields);
        dynamicMapper.insert(params);

        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            for (String tagId : req.getTagIds()) {
                tagMapper.insertEntryTag(UUID.randomUUID().toString().replace("-", ""), type.getId(), id, tagId);
            }
        }

        return buildEntry(type, id, req.getEntryName(), fields, req.getFields());
    }

    @Override
    @Transactional
    public DictEntry updateEntry(EntrySaveRequest req) {
        if (req.getId() == null || req.getId().isBlank()) {
            throw BusinessException.badRequest("id is required for update");
        }
        DictType type = requireType(req.getTypeCode());
        List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId());
        validateFields(req.getFields(), fields, false);
        validateFieldTypes(req.getFields(), fields);

        Map<String, Object> params = buildDataParams(type.getTableName(), req.getId(), req.getEntryName(), fields, req.getFields());
        params.put("fieldMappings", fields);

        int updated = dynamicMapper.update(params);
        if (updated == 0) {
            throw BusinessException.notFound("Entry", req.getId());
        }

        tagMapper.deleteByEntry(type.getId(), req.getId());
        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            for (String tagId : req.getTagIds()) {
                tagMapper.insertEntryTag(UUID.randomUUID().toString().replace("-", ""), type.getId(), req.getId(), tagId);
            }
        }

        return buildEntry(type, req.getId(), req.getEntryName(), fields, req.getFields());
    }

    @Override
    @Transactional
    public void deleteEntry(String typeCode, String id) {
        DictType type = requireType(typeCode);
        tagMapper.deleteByEntry(type.getId(), id);

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", type.getTableName());
        params.put("id", id);
        int deleted = dynamicMapper.deleteById(params);
        if (deleted == 0) {
            throw BusinessException.notFound("Entry", id);
        }
    }

    @Override
    public DictEntry getEntry(String typeCode, String id) {
        DictType type = requireType(typeCode);
        List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId());

        Map<String, Object> params = new HashMap<>();
        params.put("tableName", type.getTableName());
        params.put("id", id);
        params.put("fieldMappings", fields);
        Map<String, Object> row = dynamicMapper.findById(params);
        if (row == null) {
            throw BusinessException.notFound("Entry", id);
        }

        return mapRowToEntry(row, type, fields);
    }

    @Override
    public PageResult<DictEntry> queryEntries(EntryQueryRequest req) {
        DictType type = requireType(req.getTypeCode());
        List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId());

        // count
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("tableName", type.getTableName());
        countParams.put("typeId", type.getId());
        putTagParams(countParams, req.getTagIds());
        long total = dynamicMapper.countPage(countParams);

        // query
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("tableName", type.getTableName());
        queryParams.put("typeId", type.getId());
        queryParams.put("fieldMappings", fields);
        queryParams.put("dbType", detectDbType());
        putTagParams(queryParams, req.getTagIds());
        queryParams.put("limit", Math.min(req.getPageSize(), MAX_PAGE_SIZE));
        queryParams.put("offset", req.getOffset());
        queryParams.put("endRow", req.getOffset() + Math.min(req.getPageSize(), MAX_PAGE_SIZE));
        List<Map<String, Object>> rows = dynamicMapper.queryPage(queryParams);

        List<String> entryIds = rows.stream()
                .map(r -> String.valueOf(r.get("ID")))
                .collect(Collectors.toList());

        Map<String, List<DictTag>> tagsMap = loadTagsMap(type.getId(), entryIds);

        List<DictEntry> records = rows.stream()
                .map(r -> mapRowToEntry(r, type, fields, tagsMap))
                .collect(Collectors.toList());

        return new PageResult<>(total, req.getPageNum(), Math.min(req.getPageSize(), MAX_PAGE_SIZE), records);
    }

    // ==================== Tag Management ====================

    @Override
    @Transactional
    public DictTag createTag(DictTag tag) {
        tag.setId(UUID.randomUUID().toString().replace("-", ""));
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    @Transactional
    public DictTag updateTag(DictTag tag) {
        DictTag existing = tagMapper.findById(tag.getId());
        if (existing == null) {
            throw BusinessException.notFound("Tag", tag.getId());
        }
        tagMapper.update(tag);
        return tag;
    }

    @Override
    @Transactional
    public void deleteTag(String id) {
        tagMapper.delete(id);
    }

    @Override
    public List<DictTag> listTags() {
        return tagMapper.listTags();
    }

    @Override
    public List<DictTag> listTagsByType(String typeCode) {
        DictType type = requireType(typeCode);
        return tagMapper.findTagsByTypeId(type.getId());
    }

    // ==================== Internal Helpers ====================

    private DictType requireType(String typeCode) {
        DictType type = metadataMapper.findByTypeCode(typeCode);
        if (type == null) {
            throw BusinessException.notFound("DictType", typeCode);
        }
        return type;
    }

    /**
     * Validate table/column names against injection-safe pattern.
     * Only uppercase-starting, alphanumeric + underscore names are allowed.
     */
    private void validateSafeName(String name, String fieldLabel) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            throw BusinessException.badRequest(
                fieldLabel + " has invalid format (must start with uppercase letter, followed by letters/digits/underscore): " + name);
        }
    }

    /**
     * Flatten field data into param map keyed by columnName.
     * This allows MyBatis Provider's #{columnName} placeholders to bind correctly.
     */
    private Map<String, Object> buildDataParams(String tableName, String id, String entryName,
                                                 List<DictField> fields, Map<String, Object> fieldData) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("tableName", tableName);
        params.put("id", id);
        params.put("entryName", entryName);
        params.put("dbType", detectDbType());

        if (fields != null && fieldData != null) {
            for (DictField f : fields) {
                Object val = fieldData.get(f.getFieldCode());
                if ("NUMBER".equalsIgnoreCase(f.getFieldType()) && val instanceof String) {
                    val = new java.math.BigDecimal((String) val);
                }
                params.put(f.getColumnName(), val);
            }
        }
        return params;
    }

    /**
     * Expand tagIds list into tagId0, tagId1, ... keys to match Provider's #{tagIdN} placeholders.
     */
    private void putTagParams(Map<String, Object> params, List<String> tagIds) {
        params.put("tagIds", tagIds);
        if (tagIds != null) {
            for (int i = 0; i < tagIds.size(); i++) {
                params.put("tagId" + i, tagIds.get(i));
            }
        }
    }

    private void validateFields(Map<String, Object> fieldData, List<DictField> definitions, boolean isCreate) {
        Set<String> validKeys = definitions.stream().map(DictField::getFieldCode).collect(Collectors.toSet());

        for (String key : fieldData.keySet()) {
            if (!validKeys.contains(key)) {
                throw BusinessException.badRequest("Unknown field: " + key);
            }
        }

        if (isCreate) {
            for (DictField f : definitions) {
                if (f.isRequired()) {
                    Object val = fieldData.get(f.getFieldCode());
                    if (val == null || (val instanceof String && ((String) val).isBlank())) {
                        throw BusinessException.badRequest("Required field must not be empty: " + f.getFieldCode());
                    }
                }
            }
        }
    }

    private void validateFieldTypes(Map<String, Object> fieldData, List<DictField> definitions) {
        for (DictField f : definitions) {
            Object val = fieldData.get(f.getFieldCode());
            if (val == null) continue;

            String ft = f.getFieldType().toUpperCase();
            if ("NUMBER".equals(ft) && val instanceof String) {
                try {
                    new java.math.BigDecimal((String) val);
                } catch (NumberFormatException e) {
                    throw BusinessException.badRequest("Field " + f.getFieldCode() + " is not a valid number: " + val);
                }
            }
        }
    }

    private DictEntry buildEntry(DictType type, String id, String entryName, List<DictField> fields,
                                  Map<String, Object> fieldData) {
        DictEntry entry = new DictEntry();
        entry.setId(id);
        entry.setTypeCode(type.getTypeCode());
        entry.setEntryName(entryName);

        Map<String, Object> fieldMap = new LinkedHashMap<>();
        if (fieldData != null) {
            for (DictField f : fields) {
                fieldMap.put(f.getFieldCode(), fieldData.get(f.getFieldCode()));
            }
        }
        entry.setFields(fieldMap);
        entry.setTags(tagMapper.findTagsByEntry(type.getId(), id));
        return entry;
    }

    private DictEntry mapRowToEntry(Map<String, Object> row, DictType type, List<DictField> fields) {
        return mapRowToEntry(row, type, fields, Collections.emptyMap());
    }

    private DictEntry mapRowToEntry(Map<String, Object> row, DictType type, List<DictField> fields,
                                     Map<String, List<DictTag>> tagsMap) {
        DictEntry entry = new DictEntry();
        String id = String.valueOf(row.get("ID"));
        entry.setId(id);
        entry.setTypeCode(type.getTypeCode());
        entry.setEntryName(String.valueOf(row.getOrDefault("ENTRY_NAME", "")));

        Map<String, Object> fieldMap = new LinkedHashMap<>();
        for (DictField f : fields) {
            Object val = row.get(f.getColumnName());
            // H2 returns java.sql.Date for DATE columns; normalize to String for consistent API output
            if (val instanceof java.sql.Date) {
                val = val.toString();
            }
            fieldMap.put(f.getFieldCode(), val);
        }
        entry.setFields(fieldMap);

        Object ca = row.get("CREATED_AT");
        Object ua = row.get("UPDATED_AT");
        if (ca != null) entry.setCreatedAt(parseDateTime(ca));
        if (ua != null) entry.setUpdatedAt(parseDateTime(ua));

        entry.setTags(tagsMap.getOrDefault(id, tagMapper.findTagsByEntry(type.getId(), id)));

        return entry;
    }

    private Map<String, List<DictTag>> loadTagsMap(String typeId, List<String> entryIds) {
        if (entryIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = tagMapper.findTagsForEntries(typeId, entryIds);
        Map<String, List<DictTag>> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String entryId = String.valueOf(row.get("ENTRY_ID"));
            DictTag tag = new DictTag();
            tag.setId(String.valueOf(row.get("ID")));
            tag.setTagName(String.valueOf(row.get("TAG_NAME")));
            Object cat = row.get("CREATED_AT");
            if (cat != null) tag.setCreatedAt(parseDateTime(cat));
            map.computeIfAbsent(entryId, k -> new ArrayList<>()).add(tag);
        }
        return map;
    }

    /**
     * Detect database type from the datasource URL.
     * Used by Provider classes to adapt SQL syntax (SYSDATE vs CURRENT_TIMESTAMP,
     * Oracle pagination vs LIMIT/OFFSET).
     */
    private String detectDbType() {
        if (datasourceUrl != null && datasourceUrl.contains("oracle")) {
            return "oracle";
        }
        return "mysql";
    }

    /**
     * Parse timestamp strings from H2 (fractional seconds), MySQL, and Oracle formats.
     * Oracle JDBC driver returns java.sql.Timestamp for DATE columns.
     */
    private LocalDateTime parseDateTime(Object obj) {
        if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
        String s = obj.toString();
        // H2 returns "2026-07-01 21:07:02.101225"; strip fractional part
        if (s.contains(".")) {
            s = s.substring(0, s.indexOf('.'));
        }
        s = s.replace("T", " ");
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
