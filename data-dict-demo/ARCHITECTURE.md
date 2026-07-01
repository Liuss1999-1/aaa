# Data Dictionary — One-File Developer Guide

This single file contains everything you need to understand, rebuild, extend, or debug this project. Read it top-to-bottom before writing any code.

---

## 1. What This Project Is

A **metadata-driven, multi-tenant data dictionary** backend. Instead of writing one CRUD controller per entity type, we maintain two metadata tables (DICT_TYPE, DICT_FIELD) that describe each entity's physical table and column mappings at runtime. One set of MyBatis Provider classes dynamically generates SQL based on this metadata.

**Stack:** Java 17 + Spring Boot 3.2.5 + MyBatis 3.0.3 + Oracle (primary) / MySQL / H2  
**Test DB:** H2 in-memory (no Oracle/MySQL needed for tests)  
**Swagger:** http://localhost:8080/swagger-ui/index.html

## 2. Quick Start

```bash
# Prerequisites: JDK 17+, Maven 3.8+, Oracle database (or H2 for dev)
cd data-dict-demo

# Oracle mode (default):
# 1. Edit src/main/resources/application.yml — fill in Oracle connection
# 2. Run init-oracle.sql manually against your Oracle instance
# 3. mvn spring-boot:run

# H2 mode (zero-config, for quick dev):
# 1. Edit application.yml: change spring.sql.init.schema-locations to classpath:sql/init-h2.sql
#    and datasource.url to jdbc:h2:mem:datadict;MODE=Oracle;DB_CLOSE_DELAY=-1
#    and driver-class-name to org.h2.Driver
# 2. mvn spring-boot:run

# Run tests (always use H2):
mvn test
```

## 3. Architecture — How It Works

### The Core Idea

```
Request: POST /api/dict/entry  { "typeCode": "PRODUCT", "fields": {"price": 99} }
    │
    ▼
DictServiceImpl.createEntry()
    │
    ├── 1. Query DICT_TYPE by typeCode → get tableName="T_PRODUCT", typeId="type001"
    ├── 2. Query DICT_FIELD by typeId → get mappings:
    │      fieldCode="price" → columnName="C_PRICE", fieldType="NUMBER"
    │      fieldCode="description" → columnName="C_DESC", fieldType="TEXT"
    ├── 3. Validate: all field keys in request exist in DICT_FIELD whitelist
    ├── 4. Build params map:
    │      { tableName: "T_PRODUCT", id: "abc123", entryName: "...",
    │        C_PRICE: BigDecimal(99), C_DESC: "some text", dbType: "oracle" }
    ├── 5. Call DictDynamicMapper.insert(params)
    │      → InsertProvider reads fieldMappings + column values
    │      → Generates: INSERT INTO T_PRODUCT (ID, ENTRY_NAME, C_PRICE, C_DESC,
    │                    CREATED_AT, UPDATED_AT) VALUES (#{id}, #{entryName},
    │                    #{C_PRICE}, #{C_DESC}, SYSDATE, SYSDATE)
    │      → MyBatis executes via PreparedStatement
    └── 6. Insert tag associations into DICT_ENTRY_TAG
```

**Key insight:** The SAME code path handles a 2-field PRODUCT, a 7-field CUSTOMER, or a 12-field CLA_PROJECT. The difference is only in what DICT_TYPE and DICT_FIELD return.

### Tag Intersection Filtering

```sql
-- When tagIds = ["A", "B"]: must have BOTH tags
SELECT e.* FROM T_PRODUCT e
INNER JOIN DICT_ENTRY_TAG tg0 ON tg0.TYPE_ID=? AND tg0.ENTRY_ID=e.ID AND tg0.TAG_ID=?
INNER JOIN DICT_ENTRY_TAG tg1 ON tg1.TYPE_ID=? AND tg1.ENTRY_ID=e.ID AND tg1.TAG_ID=?
ORDER BY e.CREATED_AT DESC
```

Multiple INNER JOINs on the same table = intersection semantics.

### Oracle vs MySQL/H2 Adaptation

The `dbType` parameter (auto-detected from datasource URL) flows through all Provider methods:

| Feature | Oracle | MySQL / H2 |
|---------|--------|------------|
| Timestamp | `SYSDATE` | `CURRENT_TIMESTAMP` |
| Pagination | `ROWNUM` subquery: `WHERE rn > #{offset} AND rn <= #{endRow}` | `LIMIT #{limit} OFFSET #{offset}` |
| ID generation | UUID in Java layer (same) | UUID in Java layer (same) |
| DATE column JDBC type | `java.sql.Timestamp` | `java.sql.Date` (H2) / `String` (MySQL) |

`detectDbType()` in DictServiceImpl checks if `datasourceUrl` contains "oracle".

## 4. Database Schema

### 4.1 Metadata Tables (always present, managed via API)

```sql
CREATE TABLE DICT_TYPE (
    ID          VARCHAR2(32) PRIMARY KEY,     -- UUID, dashes stripped
    TYPE_CODE   VARCHAR2(64) NOT NULL UNIQUE, -- Stable code, e.g. "PRODUCT"
    TYPE_NAME   VARCHAR2(128) NOT NULL,       -- Display name
    TABLE_NAME  VARCHAR2(64) NOT NULL,        -- Physical table, e.g. "T_PRODUCT"
    CREATED_AT  DATE DEFAULT SYSDATE,
    UPDATED_AT  DATE DEFAULT SYSDATE
);

CREATE TABLE DICT_FIELD (
    ID          VARCHAR2(32) PRIMARY KEY,
    TYPE_ID     VARCHAR2(32) NOT NULL,        -- FK → DICT_TYPE.ID
    FIELD_CODE  VARCHAR2(64) NOT NULL,        -- API-layer field name, e.g. "price"
    FIELD_NAME  VARCHAR2(128) NOT NULL,       -- Display name
    FIELD_TYPE  VARCHAR2(32) NOT NULL,        -- TEXT / NUMBER / DATE / BOOL
    COLUMN_NAME VARCHAR2(64) NOT NULL,        -- Physical column, e.g. "C_PRICE"
    SORT_ORDER  NUMBER(3) DEFAULT 0,
    IS_REQUIRED CHAR(1) DEFAULT '0',          -- '1' = required
    CREATED_AT  DATE DEFAULT SYSDATE,
    CONSTRAINT FK_FIELD_TYPE FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID)
);
```

### 4.2 Tag Tables

```sql
CREATE TABLE DICT_TAG (
    ID          VARCHAR2(32) PRIMARY KEY,
    TAG_NAME    VARCHAR2(64) NOT NULL UNIQUE,
    CREATED_AT  DATE DEFAULT SYSDATE
);

CREATE TABLE DICT_ENTRY_TAG (
    ID          VARCHAR2(32) PRIMARY KEY,
    TYPE_ID     VARCHAR2(32) NOT NULL,        -- Together with ENTRY_ID, uniquely
    ENTRY_ID    VARCHAR2(32) NOT NULL,        -- identifies an entry across tables
    TAG_ID      VARCHAR2(32) NOT NULL,
    CONSTRAINT FK_ET_TYPE FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID),
    CONSTRAINT FK_ET_TAG FOREIGN KEY (TAG_ID) REFERENCES DICT_TAG(ID),
    CONSTRAINT UQ_ET UNIQUE (TYPE_ID, ENTRY_ID, TAG_ID)
);
```

### 4.3 Data Table Template

Every physical table MUST have these 4 columns. The rest is freeform:

```sql
CREATE TABLE T_<NAME> (
    ID          VARCHAR2(32) PRIMARY KEY,     -- UUID set by ServiceImpl
    ENTRY_NAME  VARCHAR2(256) NOT NULL,       -- Required common field
    -- ... business columns ...               -- Registered in DICT_FIELD
    CREATED_AT  DATE DEFAULT SYSDATE,         -- Auto-set by Provider SQL
    UPDATED_AT  DATE DEFAULT SYSDATE
);
```

### 4.4 Adding a New Type (zero code changes)

```sql
-- Step 1: Create physical table
CREATE TABLE T_MY_NEW_TYPE (
    ID VARCHAR2(32) PRIMARY KEY,
    ENTRY_NAME VARCHAR2(256) NOT NULL,
    C_MY_FIELD VARCHAR2(128),     -- business column
    C_MY_NUMBER NUMBER(10),
    CREATED_AT DATE DEFAULT SYSDATE,
    UPDATED_AT DATE DEFAULT SYSDATE
);

-- Step 2: Register type
INSERT INTO DICT_TYPE (ID, TYPE_CODE, TYPE_NAME, TABLE_NAME)
VALUES ('type008', 'MY_TYPE', 'My Type', 'T_MY_NEW_TYPE');

-- Step 3: Register fields (fieldCode → columnName mapping)
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED)
VALUES ('f051', 'type008', 'myField',  'My Field',  'TEXT',   'C_MY_FIELD',  1, '1');
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED)
VALUES ('f052', 'type008', 'myNumber', 'My Number', 'NUMBER', 'C_MY_NUMBER', 2, '0');

-- Done. POST /api/dict/entry with typeCode="MY_TYPE" works immediately.
```

## 5. Project File Map

```
data-dict-demo/
├── pom.xml                        # Dependencies: spring-boot 3.2.5, mybatis 3.0.3, ojdbc8, h2(test)
├── ARCHITECTURE.md                # THIS FILE — the single source of truth
├── API.md                         # Full REST API reference with examples
├── src/main/resources/
│   ├── application.yml            # Oracle connection config (fill in before running)
│   ├── sql/init-oracle.sql        # Oracle DDL + seed data (run manually first time)
│   └── sql/init-h2.sql            # H2 DDL + seed data (for zero-config dev mode)
├── src/test/resources/
│   └── application.yml            # H2 test config (auto-used by mvn test)
├── src/main/java/com/example/datadict/
│   ├── DataDictApplication.java   # @SpringBootApplication
│   ├── controller/
│   │   └── DictController.java    # 16 endpoints, Swagger @Operation annotated
│   ├── service/
│   │   ├── DictService.java       # Interface
│   │   └── impl/
│   │       └── DictServiceImpl.java  # CORE: metadata lookup → validation → SQL dispatch
│   ├── mapper/
│   │   ├── DictMetadataMapper.java   # CRUD on DICT_TYPE/DICT_FIELD (annotation-based)
│   │   ├── DictDynamicMapper.java    # Dynamic SQL via @SelectProvider/@InsertProvider
│   │   └── DictTagMapper.java        # Tag + entry-tag CRUD
│   ├── model/
│   │   ├── DictType.java, DictField.java, DictTag.java, DictEntry.java
│   │   ├── TypeSaveRequest.java, FieldSaveRequest.java
│   │   ├── EntrySaveRequest.java, EntryQueryRequest.java
│   │   ├── ApiResponse.java, PageResult.java, TypeDetail.java
│   ├── provider/
│   │   ├── InsertProvider.java    # Dynamic INSERT with SYSDATE/CURRENT_TIMESTAMP
│   │   ├── UpdateProvider.java    # Dynamic UPDATE
│   │   ├── QueryProvider.java     # Dynamic SELECT + tag joins + Oracle ROWNUM pagination
│   │   └── DeleteProvider.java    # Dynamic DELETE
│   ├── exception/
│   │   ├── BusinessException.java # 400/404/409 with factory methods
│   │   └── GlobalExceptionHandler.java
│   └── config/
│       └── JacksonConfig.java     # Optional Jackson config
└── src/test/java/com/example/datadict/
    └── DictCrudTest.java          # 12 tests covering all 7 types + security
```

## 6. Key Code Patterns

### 6.1 How Entries Flow Through the System (ServiceImpl)

```java
// DictServiceImpl.createEntry() — simplified
public DictEntry createEntry(EntrySaveRequest req) {
    DictType type = requireType(req.getTypeCode());                    // 1. Look up type
    List<DictField> fields = metadataMapper.findFieldsByTypeId(type.getId()); // 2. Get column mappings
    validateFields(req.getFields(), fields, true);                     // 3. Validate field keys
    validateFieldTypes(req.getFields(), fields);                       // 4. Validate NUMBER types

    String id = UUID.randomUUID().toString().replace("-", "");         // 5. Generate ID

    Map<String, Object> params = buildDataParams(                      // 6. Build param map
        type.getTableName(), id, req.getEntryName(), fields, req.getFields());
    // buildDataParams flattens {price: 99} → {C_PRICE: BigDecimal(99)}
    // and adds tableName, id, entryName, dbType

    dynamicMapper.insert(params);                                      // 7. Execute dynamic SQL

    for (String tagId : tagIds) {                                      // 8. Insert tag associations
        tagMapper.insertEntryTag(...);
    }

    return buildEntry(type, id, req.getEntryName(), fields, req.getFields());
}
```

### 6.2 How Dynamic SQL Is Generated (InsertProvider)

```java
// InsertProvider.insert() — simplified
public String insert(Map<String, Object> params) {
    String tableName = (String) params.get("tableName");
    List<DictField> fields = (List<DictField>) params.get("fieldMappings");
    String dbType = (String) params.getOrDefault("dbType", "mysql");
    String now = "oracle".equalsIgnoreCase(dbType) ? "SYSDATE" : "CURRENT_TIMESTAMP";

    // Build: INSERT INTO T_PRODUCT (ID, ENTRY_NAME, C_PRICE, C_DESC, CREATED_AT, UPDATED_AT)
    //        VALUES (#{id}, #{entryName}, #{C_PRICE}, #{C_DESC}, SYSDATE, SYSDATE)
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(tableName).append(" (ID, ENTRY_NAME");
    for (DictField f : fields) sql.append(", ").append(f.getColumnName());
    sql.append(", CREATED_AT, UPDATED_AT) VALUES (#{id}, #{entryName}");
    for (DictField f : fields) sql.append(", #{").append(f.getColumnName()).append("}");
    sql.append(", ").append(now).append(", ").append(now).append(")");
    return sql.toString();
}
```

### 6.3 How Tag Intersection Works (QueryProvider)

```java
// QueryProvider.queryPage() — tag join portion
if (tagIds != null && !tagIds.isEmpty()) {
    for (int i = 0; i < tagIds.size(); i++) {
        // Each tagId gets its own INNER JOIN = intersection
        sql.append(" INNER JOIN DICT_ENTRY_TAG tg").append(i);
        sql.append(" ON tg").append(i).append(".TYPE_ID = #{typeId}");
        sql.append(" AND tg").append(i).append(".ENTRY_ID = e.ID");
        sql.append(" AND tg").append(i).append(".TAG_ID = #{tagId").append(i).append("}");
    }
}
```

### 6.4 Oracle Pagination

```java
if (oracle) {
    // Wrap with ROWNUM subquery (compatible with Oracle 9i+)
    // SELECT * FROM (SELECT t.*, ROWNUM rn FROM (... ORDER BY ...) t)
    // WHERE rn > #{offset} AND rn <= #{endRow}
    sql.insert(0, "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (");
    sql.append(") t) WHERE rn > #{offset} AND rn <= #{endRow}");
} else {
    sql.append(" LIMIT #{limit} OFFSET #{offset}");
}
```

## 7. Security Model

### What IS Protected
- **Table/column names**: Never accepted from user input. Always sourced from DICT_TYPE/DICT_FIELD (database whitelist).
- **Data values**: All values use MyBatis `#{}` PreparedStatement binding — no string concatenation for values.
- **Type/field registration**: `validateSafeName()` enforces `^[A-Z][A-Z0-9_]{0,63}$` on table_name and column_name.
- **Page size**: Capped at 200 (MAX_PAGE_SIZE).
- **Field validation**: Unknown field codes rejected before any SQL is generated.
- **NEW: dbType detection**: `detectDbType()` reads from datasource URL — no user-controllable input.

### What Is NOT Protected (demo scope)
- No authentication / authorization
- No rate limiting
- Type registration API is open

### Rules for New Code
- NEVER accept tableName or columnName from request bodies directly
- NEVER concatenate user input into SQL strings — always use #{}
- When adding new Provider methods, always add `dbType` param and handle Oracle vs others
- Run `mvn test` after any Provider change — the test suite catches SQL generation bugs

## 8. API Reference (all under /api/dict)

| Method | Path | Purpose |
|--------|------|---------|
| POST | /type | Register a dictionary type |
| PUT | /type | Update a type |
| DELETE | /type/{typeCode} | Delete type (cascade) |
| GET | /type/{typeCode} | Get type detail + fields |
| GET | /type | List all types |
| POST | /type/{typeCode}/field | Add field definition |
| PUT | /type/{typeCode}/field | Update field definition |
| DELETE | /type/{typeCode}/field/{fieldCode} | Delete field |
| POST | /entry | Create entry |
| PUT | /entry | Update entry (id required) |
| DELETE | /entry/{typeCode}/{id} | Delete entry |
| GET | /entry/{typeCode}/{id} | Get single entry |
| POST | /entry/query | Paginated list + tag intersection filter |
| POST | /tag | Create tag |
| PUT | /tag | Update tag |
| DELETE | /tag/{id} | Delete tag |
| GET | /tag | List all tags |
| GET | /tag/{typeCode} | Tags used by a type |

### Entry Create Request
```json
POST /api/dict/entry
{
  "typeCode": "PRODUCT",
  "entryName": "iPhone 15",
  "fields": { "price": 6999, "description": "Apple phone" },
  "tagIds": ["tag001", "tag003"]
}
```

### Entry Query Request
```json
POST /api/dict/entry/query
{
  "typeCode": "PRODUCT",
  "tagIds": ["tag001", "tag003"],   // intersection: both tags required. empty = no filter
  "pageNum": 1,
  "pageSize": 10                    // capped at 200
}
```

### Response Envelope
```json
{ "code": 200, "message": "success", "data": { ... } }
```
Error codes: 400 (bad request), 404 (not found), 409 (conflict), 500 (server error)

## 9. Test Suite (DictCrudTest.java)

12 tests, all using H2 in-memory DB, run completely independently:

| Test | What It Verifies |
|------|-----------------|
| testProductCrud | 2-field type: create→read→update→tag-filter-query→delete→not-found |
| testCustomerCrud | 7-field type: TEXT/NUMBER/DATE/BOOL all written and read back correctly |
| testContractCrud | 8-field type: DATE field precision |
| testFieldOptionCrud | 7-field type: multiple options sharing same fieldId |
| testFieldMappingCrud | 5-field type: same localField across two sites |
| testAppCodeCrud | 10-field type: full coverage |
| testClaProjectCrud | 12-field type: largest field count |
| testTagIntersection | Entry A has tags [1,4], Entry B has [1]; filter [1,4] returns only A |
| testUnknownFieldRejected | Passing nonexistent fieldCode → 400 |
| testCrossTypeFieldRejected | CUSTOMER fields on PRODUCT type → 400 |
| testIllegalTableNameRejected | "T_EVIL; DROP TABLE" → 400 from validateSafeName |
| testIllegalColumnNameRejected | "C_BAD'; DELETE FROM" → 400 from validateSafeName |

Run: `mvn test` (no database needed)

## 10. Database Switching

The system auto-detects Oracle vs non-Oracle from the JDBC URL in application.yml.

### Using Oracle
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:oracle:thin:@//your-host:1521/your-service
    driver-class-name: oracle.jdbc.OracleDriver
    username: your_user
    password: your_pass
  sql:
    init:
      schema-locations: classpath:sql/init-oracle.sql
```
First run: execute `sql/init-oracle.sql` manually against Oracle (it contains CREATE TABLE statements).

### Using H2 (zero-config dev)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:datadict;MODE=Oracle;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      schema-locations: classpath:sql/init-h2.sql
```
Add H2 dependency: uncomment `<groupId>com.h2database</groupId>` in pom.xml (already there, test scope — move to runtime or remove scope tag).

### dbType Detection Logic
```java
private String detectDbType() {
    if (datasourceUrl != null && datasourceUrl.contains("oracle")) {
        return "oracle";
    }
    return "mysql";  // "mysql" is the fallback for H2 and MySQL
}
```

## 11. Known Quirks & Gotchas

1. **H2 DATE → java.sql.Date**: `mapRowToEntry()` converts `java.sql.Date` to String for consistent API output.
2. **H2 timestamps with microseconds**: `parseDateTime()` strips everything after `.` before parsing.
3. **Oracle DATE → java.sql.Timestamp**: Oracle JDBC returns Timestamp even for DATE columns. `parseDateTime()` handles this (instanceof LocalDateTime check first, then falls through to string parsing which also handles Timestamp.toString()).
4. **UUID without dashes**: `UUID.randomUUID().toString().replace("-", "")` for 32-char clean strings.
5. **init.sql idempotency**: Uses `DELETE FROM` with no WHERE clause, wrapped in `SET FOREIGN_KEY_CHECKS=0`.
6. **Oracle INSERT syntax**: Oracle doesn't support multi-row INSERT with VALUES (...), (...). All seed data uses individual INSERT statements.
7. **Oracle ON UPDATE**: Oracle has no `ON UPDATE CURRENT_TIMESTAMP`. UpdateProvider explicitly sets `UPDATED_AT = SYSDATE`.
8. **Oracle pagination**: Uses ROWNUM subquery wrapping, not FETCH NEXT (for compatibility with older Oracle versions).

## 12. If You Need to Add a Feature

**Adding a new field type** (e.g., BOOLEAN, ENUM):
1. Update `validateFieldTypes()` in DictServiceImpl to validate the new type
2. Providers don't need changes — they only check NUMBER vs non-NUMBER for quoting, and all values go through `#{}` binding

**Adding a new endpoint** (e.g., batch create):
1. Add the method signature to DictService interface
2. Implement in DictServiceImpl
3. Add @PostMapping in DictController
4. If it needs dynamic SQL, add a Provider method + DictDynamicMapper method

**Adding a new filter dimension** (e.g., filter by createdAt range):
1. Add fields to EntryQueryRequest
2. Pass them through queryParams in queryEntries()
3. Add WHERE clauses in QueryProvider.queryPage()

**Switching from Oracle to MySQL permanently:**
1. pom.xml: swap ojdbc8 → mysql-connector-j
2. application.yml: change driver + URL
3. Delete the `dbType` branching in all Providers (just use CURRENT_TIMESTAMP + LIMIT/OFFSET everywhere)
4. Delete `detectDbType()` from DictServiceImpl
5. Tests still work as-is (they use H2)

## 13. Current Registered Types

Generated by init-oracle.sql / init-h2.sql:

| typeCode | Table | Fields | Field Types Used |
|----------|-------|--------|-----------------|
| PRODUCT | T_PRODUCT | 2 | NUMBER, TEXT |
| CUSTOMER | T_CUSTOMER | 7 | TEXT, NUMBER, DATE, BOOL |
| CONTRACT | T_CONTRACT | 8 | TEXT, NUMBER, DATE, BOOL |
| FIELD_OPTION | T_FIELD_OPTION | 7 | TEXT, NUMBER, BOOL |
| FIELD_MAPPING | T_FIELD_MAPPING | 5 | TEXT |
| APP_CODE | T_APP_CODE | 10 | TEXT, DATE |
| CLA_PROJECT | T_CLA_PROJECT | 12 | TEXT, DATE, NUMBER |

## 14. Dependency Versions

```xml
<parent>org.springframework.boot:spring-boot-starter-parent:3.2.5</parent>
<java.version>17</java.version>
<mybatis-spring-boot-starter>3.0.3</mybatis-spring-boot-starter>
<ojdbc8>21.10.0.0</ojdbc8>    <!-- Oracle JDBC (runtime) -->
<h2>2.2.224</h2>                <!-- H2 database (test scope) -->
<springdoc-openapi>2.3.0</springdoc-openapi>
```
