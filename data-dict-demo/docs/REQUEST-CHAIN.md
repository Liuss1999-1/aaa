# Data Dictionary — Full Request Chain (CREATE / READ / UPDATE / DELETE / QUERY)

This document traces every method call from HTTP request to SQL execution for all 5 CRUD operations. Use it to understand how the system works end-to-end, verify logic, and debug issues.

---

## 0. Data Flow Layers (top → bottom)

```
Controller  →  ServiceImpl  →  Mapper  →  Provider  →  JDBC
 (REST)         (orchestrate)   (bridge)    (gen SQL)    (execute)
```

- Controller: parameter binding, annotation-based validation (@Valid)
- ServiceImpl: metadata lookup, field translation, validation, result assembly
- Mapper: interface annotated with @SelectProvider / @InsertProvider etc.
- Provider: pure Java class that returns a SQL String from a Map params
- JDBC: MyBatis takes the Provider's SQL string + params map, binds values via PreparedStatement

---

## 1. CREATE Entry

### Request
```json
POST /api/dict/entry
{
  "typeCode": "FIELD_OPTION",
  "entryName": "",
  "fields": {
    "fieldId": "customfield_10001",
    "fieldName": "priority",
    "label": "High",
    "value": "high",
    "description": "Urgent attention",
    "sortOrder": 1,
    "isActive": "1"
  },
  "tagIds": ["tag001"]
}
```

### Controller → DictController.createEntry()
```
1. @Valid validates: @NotNull fields, @NotBlank typeCode
2. Calls dictService.createEntry(req)
```

### ServiceImpl.createEntry() [line 158]

#### Step 1: Load Metadata (2 DB queries)
```
DictMetadataMapper.findByTypeCode("FIELD_OPTION")
  → DictType{id="type004", typeCode="FIELD_OPTION", tableName="T_FIELD_OPTION", ...}

DictMetadataMapper.findFieldsByTypeId("type004")
  → List<DictField> with 7 entries:
     f018: fieldCode="fieldId"   → columnName="C_FIELD_ID"   type="TEXT"   req="1"
     f019: fieldCode="fieldName" → columnName="C_FIELD_NAME" type="TEXT"   req="0"
     f020: fieldCode="label"     → columnName="C_LABEL"      type="TEXT"   req="1"
     f021: fieldCode="value"     → columnName="C_VALUE"      type="TEXT"   req="1"
     f022: fieldCode="description"→columnName="C_DESCRIPTION" type="TEXT"  req="0"
     f023: fieldCode="sortOrder" → columnName="C_SORT_ORDER" type="NUMBER" req="0"
     f024: fieldCode="isActive"  → columnName="C_IS_ACTIVE"  type="BOOL"   req="1"
```

#### Step 2: Validate (2 methods)
```
validateFields(fieldData, fields, isCreate=true)
  → Checks every key in fields exists in DictField whitelist
  → If isCreate: checks required fields are non-null/non-blank
  → Required: fieldId, label, value, isActive

validateFieldTypes(fieldData, fields)
  → For NUMBER type fields with String values: parse as BigDecimal, throw 400 if fails
```

#### Step 3: Generate ID + Resolve entryName
```
id = UUID.randomUUID().toString().replace("-", "")
  → e.g. "a1b2c3d4e5f64789abcdef0123456789"

resolveEntryName(req, fields, isCreate=true)
  → req.entryName is "" (blank), skip
  → Try fallback keys: "name" → null, "label" → "High" ✓
  → Returns "High"
```

#### Step 4: Build Params Map (buildDataParams)
```
buildDataParams() iterates DictField list, flattens fieldCode→value into columnName→value:

params = {
  tableName:  "T_FIELD_OPTION",
  id:         "a1b2c3d4e5f64789abcdef0123456789",
  entryName:  "High",
  dbType:     "oracle" (detected from datasource URL),
  C_FIELD_ID:    "customfield_10001",
  C_FIELD_NAME:  "priority",
  C_LABEL:       "High",
  C_VALUE:       "high",
  C_DESCRIPTION: "Urgent attention",
  C_SORT_ORDER:  BigDecimal(1),    ← "1" converted because fieldType=NUMBER
  C_IS_ACTIVE:   "1"
}

params.put("fieldMappings", fields)  → the List<DictField> for Provider to iterate
```

#### Step 5: Call dynamicMapper.insert(params)
```
InsertProvider.insert(params):

1. tableName = "T_FIELD_OPTION"
2. dbType = "oracle" → now = "SYSDATE"
3. Iterates fieldMappings to append column names and #{colName} placeholders

Generated SQL:
  INSERT INTO T_FIELD_OPTION (ID, ENTRY_NAME, C_FIELD_ID, C_FIELD_NAME, C_LABEL,
    C_VALUE, C_DESCRIPTION, C_SORT_ORDER, C_IS_ACTIVE, CREATED_AT, UPDATED_AT)
  VALUES (#{id}, #{entryName}, #{C_FIELD_ID}, #{C_FIELD_NAME}, #{C_LABEL},
    #{C_VALUE}, #{C_DESCRIPTION}, #{C_SORT_ORDER}, #{C_IS_ACTIVE}, SYSDATE, SYSDATE)

4. MyBatis binds params map values to #{} placeholders via PreparedStatement
5. Execute, returns 1 (row inserted)
```

#### Step 6: Insert Tag Associations
```
For tagId in ["tag001"]:
  tagMapper.insertEntryTag(UUID, "type004", "a1b2c3d4...", "tag001")
  → INSERT INTO DICT_ENTRY_TAG (ID, TYPE_ID, ENTRY_ID, TAG_ID) VALUES (?,?,?,?)
```

#### Step 7: Return
```
buildEntry(type, id, "High", fields, fieldData):
  → DictEntry {
      id: "a1b2c3d4...",
      typeCode: "FIELD_OPTION",
      entryName: "High",
      fields: {fieldId:"customfield_10001", fieldName:"priority", label:"High", ...},
      tags: [findTagsByEntry("type004","a1b2c3d4...")] → [{tag001, "Hot"}]
    }
```

---

## 2. READ (Single Entry)

### Request
```
GET /api/dict/entry/FIELD_OPTION/a1b2c3d4e5f64789abcdef0123456789
```

### Controller → DictController.getEntry("FIELD_OPTION", id)
```
calls dictService.getEntry("FIELD_OPTION", id)
```

### ServiceImpl.getEntry() [line 233]

#### Step 1: Load Metadata
```
Same as CREATE Step 1: load type + fields from DICT_TYPE/DICT_FIELD
```

#### Step 2: Call dynamicMapper.findById(params)
```
params = {tableName:"T_FIELD_OPTION", id:"a1b2c3d4...", fieldMappings:[7 fields]}

QueryProvider.findById(params):

Generated SQL:
  SELECT ID, ENTRY_NAME, C_FIELD_ID, C_FIELD_NAME, C_LABEL, C_VALUE,
         C_DESCRIPTION, C_SORT_ORDER, C_IS_ACTIVE, CREATED_AT, UPDATED_AT
  FROM T_FIELD_OPTION WHERE ID = #{id}

Returns: Map<String, Object> row = {ID="a1b2c...", ENTRY_NAME="High",
  C_FIELD_ID="customfield_10001", C_LABEL="High", ...}
```

#### Step 3: Convert Row → DictEntry (mapRowToEntry)
```
mapRowToEntry(row, type, fields):
  1. id = row.get("ID") → "a1b2c3..."
  2. entryName = row.get("ENTRY_NAME") → "High"
  3. For each DictField, read row.get(columnName) and put into fieldMap:
     fieldMap = {fieldId:"customfield_10001", fieldName:"priority",
                 label:"High", value:"high", ...}
     → java.sql.Date values are .toString()-ed for consistent output
  4. Parse CREATED_AT / UPDATED_AT via parseDateTime()
  5. Load tags: findTagsByEntry("type004", id) → [{tag001, "Hot"}]
  6. Return DictEntry
```

---

## 3. UPDATE Entry

### Request
```json
PUT /api/dict/entry
{
  "id": "a1b2c3d4e5f64789abcdef0123456789",
  "typeCode": "FIELD_OPTION",
  "fields": {
    "label": "Low",
    "value": "low"
  },
  "tagIds": ["tag001", "tag002"]
}
```

### ServiceImpl.updateEntry() [line 186]

#### Step 1: Load Metadata (same as CREATE)
#### Step 2: Validate (same as CREATE but isCreate=false — skips required checks)
#### Step 3: Resolve entryName
```
resolveEntryName(req, fields, isCreate=false)
  → req.entryName is null (not provided) → return null

Since entryName is null → fall back to reading current entryName from DB:
  readSingleRow("T_FIELD_OPTION", id, fields)
  → SELECT ID, ENTRY_NAME, ... FROM T_FIELD_OPTION WHERE ID = #{id}
  → entryName = "High" (preserved from existing row)
```

#### Step 4: Build Params
```
buildDataParams with entryName="High" and only the fields provided:
params = {
  tableName: "T_FIELD_OPTION",
  id: "a1b2c...",
  entryName: "High",     ← preserved from DB
  dbType: "oracle",
  C_LABEL: "Low",         ← updated
  C_VALUE: "low",         ← updated
  // C_FIELD_ID, C_DESCRIPTION etc. NOT in params → Provider will set them to NULL
}
```

> ⚠️ **Known issue**: UpdateProvider sets ALL registered columns. If the caller only sends {label, value}, all other columns get set to NULL because fieldData.get() returns null for them and `buildDataParams` puts null into params. This means a partial update request will WIPE unmentioned fields. For the current FIELD_OPTION use-case (simple options), this is acceptable. For general use, the UPDATE should only touch columns that appear in the request.

#### Step 5: Execute Update
```
UpdateProvider.update(params):

Generated SQL:
  UPDATE T_FIELD_OPTION SET ENTRY_NAME = #{entryName}, UPDATED_AT = SYSDATE,
    C_FIELD_ID = #{C_FIELD_ID}, C_FIELD_NAME = #{C_FIELD_NAME},
    C_LABEL = #{C_LABEL}, C_VALUE = #{C_VALUE},
    C_DESCRIPTION = #{C_DESCRIPTION}, C_SORT_ORDER = #{C_SORT_ORDER},
    C_IS_ACTIVE = #{C_IS_ACTIVE}
  WHERE ID = #{id}

→ All columns set, including nulls for fields not in request
→ Returns 1 (updated)
```

#### Step 6: Re-associate Tags (delete-then-insert)
```
tagMapper.deleteByEntry("type004", id)
tagMapper.insertEntryTag("tag001")  // for each new tagId
tagMapper.insertEntryTag("tag002")
```

#### Step 7: Return
```
buildEntry → DictEntry with updated label="Low", value="low"
```

---

## 4. DELETE Entry

### Request
```
DELETE /api/dict/entry/FIELD_OPTION/a1b2c3d4e5f64789abcdef0123456789
```

### ServiceImpl.deleteEntry() [line 218]
```
1. requireType("FIELD_OPTION") → DictType{id="type004", tableName="T_FIELD_OPTION"}
2. tagMapper.deleteByEntry("type004", id) → remove all tag associations
3. dynamicMapper.deleteById({tableName:"T_FIELD_OPTION", id:"a1b2c..."})
   → DELETE FROM T_FIELD_OPTION WHERE ID = #{id}
4. If deleted rows == 0 → 404
```

---

## 5. QUERY (Paginated List + Tag Filter)

### Request
```json
POST /api/dict/entry/query
{
  "typeCode": "FIELD_OPTION",
  "tagIds": ["tag001"],
  "pageNum": 1,
  "pageSize": 10
}
```

### ServiceImpl.queryEntries() [line 249]

#### Step 1: Load Metadata (same as CREATE)
#### Step 2: Count
```
putTagParams(countParams, ["tag001"])
  → tagId0 = "tag001"

QueryProvider.countPage():
SELECT COUNT(*) FROM T_FIELD_OPTION e
INNER JOIN DICT_ENTRY_TAG tg0 ON tg0.TYPE_ID = #{typeId}
  AND tg0.ENTRY_ID = e.ID AND tg0.TAG_ID = #{tagId0}
→ Returns total = 2
```

#### Step 3: Query with Pagination
```
putTagParams(queryParams, ["tag001"])
  → tagId0 = "tag001"
queryParams.dbType = "oracle"
queryParams.limit = 10
queryParams.offset = 0
queryParams.endRow = 10
queryParams.fieldMappings = [7 DictField objects]

QueryProvider.queryPage():

Oracle path generates:
SELECT * FROM (
  SELECT t.*, ROWNUM rn FROM (
    SELECT e.ID, e.ENTRY_NAME, e.C_FIELD_ID AS C_FIELD_ID,
           e.C_FIELD_NAME AS C_FIELD_NAME, e.C_LABEL AS C_LABEL,
           e.C_VALUE AS C_VALUE, e.C_DESCRIPTION AS C_DESCRIPTION,
           e.C_SORT_ORDER AS C_SORT_ORDER, e.C_IS_ACTIVE AS C_IS_ACTIVE,
           e.CREATED_AT, e.UPDATED_AT
    FROM T_FIELD_OPTION e
    INNER JOIN DICT_ENTRY_TAG tg0 ON tg0.TYPE_ID = #{typeId}
      AND tg0.ENTRY_ID = e.ID AND tg0.TAG_ID = #{tagId0}
    ORDER BY e.CREATED_AT DESC
  ) t
) WHERE rn > #{offset} AND rn <= #{endRow}

Returns: List<Map<String,Object>> with 2 rows
```

#### Step 4: Batch Load Tags (no N+1)
```
entryIds = rows.stream().map(r→String.valueOf(r.get("ID"))).toList()
  → ["fo001", "fo002"]

loadTagsMap("type004", ["fo001","fo002"])
  → One batch query:
    SELECT et.ENTRY_ID, t.ID, t.TAG_NAME, t.CREATED_AT
    FROM DICT_TAG t INNER JOIN DICT_ENTRY_TAG et ON t.ID = et.TAG_ID
    WHERE et.TYPE_ID = ? AND et.ENTRY_ID IN (?, ?)

  → Results grouped by ENTRY_ID:
    fo001 → [{tag001, "Hot"}]
    fo002 → []
```

#### Step 5: Convert Rows → PageResult
```
For each row: mapRowToEntry(row, type, fields, tagsMap)
Each DictEntry gets its tags from the pre-loaded tagsMap (no extra queries)

Return PageResult{total: 2, pageNum: 1, pageSize: 10, records: [...]}
```

---

## 6. Oracle Pagination (QueryProvider Detail)

For Oracle (old versions without FETCH NEXT), ROWNUM-based pagination:

```sql
-- Raw QueryProvider output:
SELECT * FROM (
  SELECT t.*, ROWNUM rn FROM (
    SELECT e.ID, e.ENTRY_NAME, e.C_COL1 AS C_COL1, ...
    FROM T_EXAMPLE e
    [INNER JOIN DICT_ENTRY_TAG tgN ...]
    ORDER BY e.CREATED_AT DESC
  ) t
) WHERE rn > #{offset} AND rn <= #{endRow}
```

- `offset` = (pageNum - 1) × pageSize
- `endRow` = offset + pageSize
- Both values computed in `queryEntries()`, Provider just binds them

For H2/MySQL: `LIMIT #{limit} OFFSET #{offset}` — no subquery needed.

---

## 7. entryName Auto-Resolution Logic

### createEntry path
```
resolveEntryName(req, fields, isCreate=true)
  1. req.entryName provided & non-blank → use it
  2. Try fields.get("name") → if non-null, use as entryName
  3. Try fields.get("label") → if non-null, use as entryName
  4. Try fields.get("value") → if non-null, use as entryName
  5. Iterate DictField list, use first non-null field value
  6. If nothing found → throw 400
```

### updateEntry path
```
resolveEntryName(req, fields, isCreate=false)
  1. req.entryName provided & non-blank → use it (updated label → entryName follows)
  2. Otherwise return null → caller falls back to readSingleRow() to preserve existing entryName from DB
```

---

## 8. Key Parameter Flow

### buildDataParams() Translations

| Layer | Key | Example |
|-------|-----|---------|
| API Request | `fieldCode` → value | `"label"` → `"High"` |
| buildDataParams output | `columnName` → value | `"C_LABEL"` → `"High"` |
| Provider SQL | `#{columnName}` | `#{C_LABEL}` |
| JDBC PreparedStatement | `?` bound to value | `?` → `"High"` |

---

## 9. Known Issues & Caveats

### 9.1 ✅ FIXED — UPDATE Nullifies Unmentioned Fields
`buildUpdateParams` now only includes columns whose fieldCode appears in the request. The Provider receives `fieldMappings` as only the touched fields, so the generated UPDATE SQL only SETs columns that were actually sent in the request. Unmentioned fields are left unchanged in the database.

### 9.2 ✅ FIXED — DictMetadataMapper & DictTagMapper CURRENT_TIMESTAMP
`insertType()`, `insertField()`, and `tagMapper.insert()` no longer include `CREATED_AT` / `UPDATED_AT` in INSERT SQL. The database defaults (SYSDATE on Oracle, CURRENT_TIMESTAMP on H2/MySQL) handle timestamp creation automatically.

### 9.3 Oracle ROWNUM pagination returns `rn` column in result map
The Oracle pagination query wraps results with `SELECT * FROM (... WHERE ROWNUM rn ...)`. This means the result map will contain an extra key `RN` (the rownum alias). This does NOT affect the system because `mapRowToEntry` only reads keys it needs (ID, ENTRY_NAME, columnNames from DictField, CREATED_AT, UPDATED_AT). The extra `RN` key is silently ignored.

### 9.4 Delete All By Table is dangerous
`deleteAllByTable` generates `DELETE FROM {tableName}` with NO WHERE clause. Used in `deleteType()` to cascade-clean a whole type. This is intentional but worth noting.
