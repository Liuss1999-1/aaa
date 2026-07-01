package com.example.datadict;

import com.example.datadict.model.*;
import com.example.datadict.service.impl.DictServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 整体性测试：使用 H2 内存数据库，覆盖全部 7 种类型的增删改查 + 标签过滤 + 安全校验。
 * 每种类型独立测试完整的 CRUD 链路。
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DictCrudTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DictServiceImpl service;

    @BeforeEach
    void setUp() {
        // === 元数据 ===
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS DICT_TYPE (
                ID VARCHAR(32) PRIMARY KEY, TYPE_CODE VARCHAR(64) UNIQUE,
                TYPE_NAME VARCHAR(128), TABLE_NAME VARCHAR(64),
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS DICT_FIELD (
                ID VARCHAR(32) PRIMARY KEY, TYPE_ID VARCHAR(32), FIELD_CODE VARCHAR(64),
                FIELD_NAME VARCHAR(128), FIELD_TYPE VARCHAR(32), COLUMN_NAME VARCHAR(64),
                SORT_ORDER INT DEFAULT 0, IS_REQUIRED CHAR(1) DEFAULT '0',
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID)
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS DICT_TAG (
                ID VARCHAR(32) PRIMARY KEY, TAG_NAME VARCHAR(64) UNIQUE,
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS DICT_ENTRY_TAG (
                ID VARCHAR(32) PRIMARY KEY, TYPE_ID VARCHAR(32), ENTRY_ID VARCHAR(32),
                TAG_ID VARCHAR(32),
                FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID),
                FOREIGN KEY (TAG_ID) REFERENCES DICT_TAG(ID),
                UNIQUE (TYPE_ID, ENTRY_ID, TAG_ID)
            )
        """);

        // === 物理表（7 张，字段数 2/7/8/7/5/10/12） ===
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_PRODUCT (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_PRICE DECIMAL(18,4), C_DESC VARCHAR(4000), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_CUSTOMER (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_PHONE VARCHAR(20), C_EMAIL VARCHAR(128), C_COMPANY VARCHAR(256), C_AGE INT, C_VIP_LEVEL INT, C_BIRTHDAY DATE, C_IS_ACTIVE CHAR(1), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_CONTRACT (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_PARTY_A VARCHAR(256), C_PARTY_B VARCHAR(256), C_AMOUNT DECIMAL(18,2), C_SIGN_DATE DATE, C_EXPIRE_DATE DATE, C_IS_SEALED CHAR(1), C_FILE_URL VARCHAR(512), C_REMARK VARCHAR(4000), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_FIELD_OPTION (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_FIELD_ID VARCHAR(64), C_FIELD_NAME VARCHAR(128), C_VALUE VARCHAR(256), C_LABEL VARCHAR(256), C_DESCRIPTION VARCHAR(1000), C_SORT_ORDER INT, C_IS_ACTIVE CHAR(1), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_FIELD_MAPPING (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_SITE_ID VARCHAR(64), C_LOCAL_FIELD VARCHAR(128), C_REMOTE_FIELD_ID VARCHAR(128), C_REMOTE_FIELD_NAME VARCHAR(256), C_FIELD_TYPE VARCHAR(64), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_APP_CODE (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_APP_CODE VARCHAR(64), C_APP_NAME VARCHAR(128), C_DESCRIPTION VARCHAR(1000), C_OWNER VARCHAR(64), C_TEAM VARCHAR(128), C_LANGUAGE VARCHAR(32), C_REPO_URL VARCHAR(512), C_DEPLOY_ENV VARCHAR(64), C_STATUS VARCHAR(32), C_RELEASE_DATE DATE, CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS T_CLA_PROJECT (ID VARCHAR(32) PRIMARY KEY, ENTRY_NAME VARCHAR(256) NOT NULL, C_PROJECT_CODE VARCHAR(64), C_ORGANIZATION VARCHAR(256), C_MAINTAINER VARCHAR(64), C_LICENSE VARCHAR(128), C_REPO_URL VARCHAR(512), C_SIGN_DATE DATE, C_EXPIRE_DATE DATE, C_APPROVAL_STATUS VARCHAR(32), C_CLA_TYPE VARCHAR(32), C_SIGNATORY_COUNT INT, C_REMARK VARCHAR(4000), CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // === 注册 7 种类型 ===
        regType("type001","PRODUCT","T_PRODUCT");
        regType("type002","CUSTOMER","T_CUSTOMER");
        regType("type003","CONTRACT","T_CONTRACT");
        regType("type004","FIELD_OPTION","T_FIELD_OPTION");
        regType("type005","FIELD_MAPPING","T_FIELD_MAPPING");
        regType("type006","APP_CODE","T_APP_CODE");
        regType("type007","CLA_PROJECT","T_CLA_PROJECT");

        // === 注册字段 ===
        // PRODUCT: 2
        regField("f001","type001","price","NUMBER","C_PRICE",1,"1");
        regField("f002","type001","description","TEXT","C_DESC",2,"0");
        // CUSTOMER: 7
        regField("f003","type002","phone","TEXT","C_PHONE",1,"1");
        regField("f004","type002","email","TEXT","C_EMAIL",2,"0");
        regField("f005","type002","company","TEXT","C_COMPANY",3,"0");
        regField("f006","type002","age","NUMBER","C_AGE",4,"0");
        regField("f007","type002","vipLevel","NUMBER","C_VIP_LEVEL",5,"0");
        regField("f008","type002","birthday","DATE","C_BIRTHDAY",6,"0");
        regField("f009","type002","isActive","BOOL","C_IS_ACTIVE",7,"1");
        // CONTRACT: 8
        regField("f010","type003","partyA","TEXT","C_PARTY_A",1,"1");
        regField("f011","type003","partyB","TEXT","C_PARTY_B",2,"1");
        regField("f012","type003","amount","NUMBER","C_AMOUNT",3,"1");
        regField("f013","type003","signDate","DATE","C_SIGN_DATE",4,"1");
        regField("f014","type003","expireDate","DATE","C_EXPIRE_DATE",5,"0");
        regField("f015","type003","isSealed","BOOL","C_IS_SEALED",6,"0");
        regField("f016","type003","fileUrl","TEXT","C_FILE_URL",7,"0");
        regField("f017","type003","remark","TEXT","C_REMARK",8,"0");
        // FIELD_OPTION: 7
        regField("f018","type004","fieldId","TEXT","C_FIELD_ID",1,"1");
        regField("f019","type004","fieldName","TEXT","C_FIELD_NAME",2,"0");
        regField("f020","type004","label","TEXT","C_LABEL",3,"1");
        regField("f021","type004","value","TEXT","C_VALUE",4,"1");
        regField("f022","type004","description","TEXT","C_DESCRIPTION",5,"0");
        regField("f023","type004","sortOrder","NUMBER","C_SORT_ORDER",6,"0");
        regField("f024","type004","isActive","BOOL","C_IS_ACTIVE",7,"1");
        // FIELD_MAPPING: 5
        regField("f025","type005","siteId","TEXT","C_SITE_ID",1,"1");
        regField("f026","type005","localField","TEXT","C_LOCAL_FIELD",2,"1");
        regField("f027","type005","remoteFieldId","TEXT","C_REMOTE_FIELD_ID",3,"1");
        regField("f028","type005","remoteFieldName","TEXT","C_REMOTE_FIELD_NAME",4,"0");
        regField("f029","type005","fieldType","TEXT","C_FIELD_TYPE",5,"0");
        // APP_CODE: 10
        regField("f030","type006","appCode","TEXT","C_APP_CODE",1,"1");
        regField("f031","type006","appName","TEXT","C_APP_NAME",2,"1");
        regField("f032","type006","description","TEXT","C_DESCRIPTION",3,"0");
        regField("f033","type006","owner","TEXT","C_OWNER",4,"1");
        regField("f034","type006","team","TEXT","C_TEAM",5,"0");
        regField("f035","type006","language","TEXT","C_LANGUAGE",6,"0");
        regField("f036","type006","repoUrl","TEXT","C_REPO_URL",7,"0");
        regField("f037","type006","deployEnv","TEXT","C_DEPLOY_ENV",8,"0");
        regField("f038","type006","status","TEXT","C_STATUS",9,"1");
        regField("f039","type006","releaseDate","DATE","C_RELEASE_DATE",10,"0");
        // CLA_PROJECT: 11 （projectName 不用注册，对应 ENTRY_NAME）
        regField("f040","type007","projectCode","TEXT","C_PROJECT_CODE",1,"1");
        regField("f041","type007","organization","TEXT","C_ORGANIZATION",2,"1");
        regField("f042","type007","maintainer","TEXT","C_MAINTAINER",3,"1");
        regField("f043","type007","license","TEXT","C_LICENSE",4,"0");
        regField("f044","type007","repoUrl","TEXT","C_REPO_URL",5,"0");
        regField("f045","type007","signDate","DATE","C_SIGN_DATE",6,"0");
        regField("f046","type007","expireDate","DATE","C_EXPIRE_DATE",7,"0");
        regField("f047","type007","approvalStatus","TEXT","C_APPROVAL_STATUS",8,"1");
        regField("f048","type007","claType","TEXT","C_CLA_TYPE",9,"1");
        regField("f049","type007","signatoryCount","NUMBER","C_SIGNATORY_COUNT",10,"0");
        regField("f050","type007","remark","TEXT","C_REMARK",11,"0");

        // === 标签 ===
        regTag("tag001","热门");
        regTag("tag002","冷门");
        regTag("tag003","VIP");
        regTag("tag004","重要");
        regTag("tag005","已过期");
    }

    // ==================== 1. PRODUCT（2 字段） ====================
    @Test @Order(1)
    void testProductCrud() {
        DictEntry e = service.createEntry(buildReq("PRODUCT","测试产品",
                map2("price",9999,"description","测试描述"),List.of("tag001","tag003")));
        assertEquals(2, e.getFields().size());
        assertEquals(9999, ((Number) e.getFields().get("price")).intValue());
        assertEquals(2, e.getTags().size());

        String id = e.getId();
        DictEntry read = service.getEntry("PRODUCT", id);
        assertEquals("测试产品", read.getEntryName());

        DictEntry upd = service.updateEntry(buildUpdReq("PRODUCT",id,"更新产品",
                map2("price",8888,"description","更新描述"),List.of("tag002")));
        assertEquals("更新产品", upd.getEntryName());
        assertEquals(1, upd.getTags().size());
        assertEquals("冷门", upd.getTags().get(0).getTagName());

        // 标签过滤
        EntryQueryRequest q = new EntryQueryRequest();
        q.setTypeCode("PRODUCT"); q.setTagIds(List.of("tag002")); q.setPageNum(1); q.setPageSize(10);
        PageResult<DictEntry> page = service.queryEntries(q);
        assertTrue(page.getTotal() >= 1);

        service.deleteEntry("PRODUCT", id);
        assertNotFound("PRODUCT", id);
    }

    // ==================== 2. CUSTOMER（7 字段，TEXT/NUMBER/DATE/BOOL） ====================
    @Test @Order(2)
    void testCustomerCrud() {
        Map<String,Object> create = new LinkedHashMap<>();
        create.put("phone","13800000000");
        create.put("email","x@x.com");
        create.put("company","阿里");
        create.put("age",28);
        create.put("vipLevel",3);
        create.put("birthday","1998-05-15");
        create.put("isActive","1");

        DictEntry e = service.createEntry(buildReq("CUSTOMER","张三",create,List.of("tag004")));
        assertEquals(7, e.getFields().size());
        assertEquals("阿里", e.getFields().get("company"));

        String id = e.getId();

        // 查
        DictEntry read = service.getEntry("CUSTOMER", id);
        assertEquals("张三", read.getEntryName());
        assertEquals("1998-05-15", read.getFields().get("birthday"));

        // 改
        Map<String,Object> upd = new LinkedHashMap<>();
        upd.put("phone","13900000000");
        upd.put("email","y@y.com");
        upd.put("company","腾讯");
        upd.put("age",35);
        upd.put("vipLevel",5);
        upd.put("birthday","1991-08-20");
        upd.put("isActive","0");
        DictEntry updated = service.updateEntry(buildUpdReq("CUSTOMER",id,"李四",upd,List.of()));
        assertEquals("腾讯", updated.getFields().get("company"));
        assertEquals(0, updated.getTags().size());

        // 删
        service.deleteEntry("CUSTOMER", id);
        assertNotFound("CUSTOMER", id);
    }

    // ==================== 3. CONTRACT（8 字段） ====================
    @Test @Order(3)
    void testContractCrud() {
        Map<String,Object> f = new LinkedHashMap<>();
        f.put("partyA","甲方A");
        f.put("partyB","乙方B");
        f.put("amount",500000);
        f.put("signDate","2026-01-15");
        f.put("expireDate","2026-12-31");
        f.put("isSealed","1");
        f.put("fileUrl","https://oss.example.com/ct.pdf");
        f.put("remark","测试合同");

        DictEntry e = service.createEntry(buildReq("CONTRACT","测试合同",f,List.of("tag004")));
        assertEquals(8, e.getFields().size());
        assertEquals("甲方A", e.getFields().get("partyA"));

        DictEntry read = service.getEntry("CONTRACT", e.getId());
        assertEquals("2026-01-15", read.getFields().get("signDate"));
        assertEquals("https://oss.example.com/ct.pdf", read.getFields().get("fileUrl"));

        service.deleteEntry("CONTRACT", e.getId());
        assertNotFound("CONTRACT", e.getId());
    }

    // ==================== 4. FIELD_OPTION（7 字段） ====================
    @Test @Order(4)
    void testFieldOptionCrud() {
        Map<String,Object> f = new LinkedHashMap<>();
        f.put("fieldId","customfield_10001");
        f.put("fieldName","priority");
        f.put("label","高");
        f.put("value","high");
        f.put("description","紧急处理");
        f.put("sortOrder",1);
        f.put("isActive","1");

        DictEntry e = service.createEntry(buildReq("FIELD_OPTION","优先级-High",f,List.of("tag001")));
        assertEquals(7, e.getFields().size());
        assertEquals("priority", e.getFields().get("fieldName"));
        assertEquals("high", e.getFields().get("value"));

        // 更多 option 示例
        Map<String,Object> f2 = new LinkedHashMap<>();
        f2.put("fieldId","customfield_10001");
        f2.put("fieldName","priority");
        f2.put("label","低");
        f2.put("value","low");
        f2.put("description","可延后");
        f2.put("sortOrder",3);
        f2.put("isActive","1");
        DictEntry e2 = service.createEntry(buildReq("FIELD_OPTION","优先级-Low",f2,List.of()));

        // 列表查询，确认同一个 fieldId 下有多个 option
        PageResult<DictEntry> page = service.queryEntries(queryReq("FIELD_OPTION"));
        assertTrue(page.getTotal() >= 2);

        service.deleteEntry("FIELD_OPTION", e2.getId());
        service.deleteEntry("FIELD_OPTION", e.getId());
    }

    // ==================== 5. FIELD_MAPPING（5 字段） ====================
    @Test @Order(5)
    void testFieldMappingCrud() {
        Map<String,Object> f = new LinkedHashMap<>();
        f.put("siteId","jira-prod");
        f.put("localField","priority");
        f.put("remoteFieldId","customfield_10001");
        f.put("remoteFieldName","Priority");
        f.put("fieldType","select");

        DictEntry e = service.createEntry(buildReq("FIELD_MAPPING","优先级-生产映射",f,List.of("tag004")));
        assertEquals(5, e.getFields().size());
        assertEquals("jira-prod", e.getFields().get("siteId"));
        assertEquals("priority", e.getFields().get("localField"));
        assertEquals("customfield_10001", e.getFields().get("remoteFieldId"));

        // 同字段不同站点
        Map<String,Object> f2 = new LinkedHashMap<>();
        f2.put("siteId","jira-test");
        f2.put("localField","priority");
        f2.put("remoteFieldId","customfield_20005");
        f2.put("remoteFieldName","优先级");
        f2.put("fieldType","select");
        DictEntry e2 = service.createEntry(buildReq("FIELD_MAPPING","优先级-测试映射",f2,List.of()));

        // 两个映射条目共用同一个 localField
        PageResult<DictEntry> page = service.queryEntries(queryReq("FIELD_MAPPING"));
        assertTrue(page.getTotal() >= 2);

        service.deleteEntry("FIELD_MAPPING", e2.getId());
        service.deleteEntry("FIELD_MAPPING", e.getId());
    }

    // ==================== 6. APP_CODE（10 字段） ====================
    @Test @Order(6)
    void testAppCodeCrud() {
        Map<String,Object> f = new LinkedHashMap<>();
        f.put("appCode","USER-SVC");
        f.put("appName","用户服务");
        f.put("description","统一用户管理");
        f.put("owner","张三");
        f.put("team","基础架构组");
        f.put("language","Java");
        f.put("repoUrl","https://git.example.com/user-svc");
        f.put("deployEnv","K8s");
        f.put("status","已上线");
        f.put("releaseDate","2025-03-15");

        DictEntry e = service.createEntry(buildReq("APP_CODE","用户服务",f,List.of("tag001","tag004")));
        assertEquals(10, e.getFields().size());
        assertEquals("张三", e.getFields().get("owner"));
        assertEquals("已上线", e.getFields().get("status"));

        DictEntry read = service.getEntry("APP_CODE", e.getId());
        assertEquals("https://git.example.com/user-svc", read.getFields().get("repoUrl"));

        service.deleteEntry("APP_CODE", e.getId());
        assertNotFound("APP_CODE", e.getId());
    }

    // ==================== 7. CLA_PROJECT（11 字段，最多） ====================
    @Test @Order(7)
    void testClaProjectCrud() {
        Map<String,Object> f = new LinkedHashMap<>();
        f.put("projectCode","SKY-CLA");
        f.put("organization","Apache");
        f.put("maintainer","张三");
        f.put("license","Apache 2.0");
        f.put("repoUrl","https://git.example.com/sky");
        f.put("signDate","2025-01-10");
        f.put("expireDate","2026-01-10");
        f.put("approvalStatus","已通过");
        f.put("claType","企业CLA");
        f.put("signatoryCount",45);
        f.put("remark","年度续签");

        DictEntry e = service.createEntry(buildReq("CLA_PROJECT","开源项目Sky",f,List.of("tag001","tag004")));
        assertEquals(11, e.getFields().size());
        assertEquals("Apache", e.getFields().get("organization"));
        assertEquals(45, ((Number) e.getFields().get("signatoryCount")).intValue());

        String id = e.getId();
        DictEntry read = service.getEntry("CLA_PROJECT", id);
        assertEquals("已通过", read.getFields().get("approvalStatus"));

        service.deleteEntry("CLA_PROJECT", id);
        assertNotFound("CLA_PROJECT", id);
    }

    // ==================== 8. 标签交集过滤 ====================
    @Test @Order(8)
    void testTagIntersection() {
        Map<String,Object> f1 = new LinkedHashMap<>();
        f1.put("appCode","A"); f1.put("appName","AApp"); f1.put("owner","p1"); f1.put("status","已上线");
        DictEntry eA = service.createEntry(buildReq("APP_CODE","A应用",f1,List.of("tag001","tag004")));

        Map<String,Object> f2 = new LinkedHashMap<>();
        f2.put("appCode","B"); f2.put("appName","BApp"); f2.put("owner","p2"); f2.put("status","已上线");
        DictEntry eB = service.createEntry(buildReq("APP_CODE","B应用",f2,List.of("tag001")));

        // tag001+tag004 交集 → 只含 eA 不含 eB
        EntryQueryRequest q = new EntryQueryRequest();
        q.setTypeCode("APP_CODE");
        q.setTagIds(List.of("tag001","tag004"));
        q.setPageNum(1);
        q.setPageSize(10);
        PageResult<DictEntry> page = service.queryEntries(q);
        List<String> ids = page.getRecords().stream().map(DictEntry::getId).toList();
        assertTrue(ids.contains(eA.getId()));
        assertFalse(ids.contains(eB.getId()));

        service.deleteEntry("APP_CODE", eB.getId());
        service.deleteEntry("APP_CODE", eA.getId());
    }

    // ==================== 9. 安全校验 ====================
    @Test @Order(9)
    void testUnknownFieldRejected() {
        EntrySaveRequest req = buildReq("PRODUCT","测试",map2("price",100,"nonexistent","x"),List.of());
        var ex = assertThrows(Exception.class, () -> service.createEntry(req));
        assertTrue(ex.getMessage().contains("未知字段"));
    }

    @Test @Order(10)
    void testCrossTypeFieldRejected() {
        // CUSTOMER 的字段不能用于 PRODUCT
        EntrySaveRequest req = buildReq("PRODUCT","测试",map2("phone","138","company","x"),List.of());
        var ex = assertThrows(Exception.class, () -> service.createEntry(req));
        assertTrue(ex.getMessage().contains("未知字段"));
    }

    @Test @Order(11)
    void testIllegalTableNameRejected() {
        TypeSaveRequest r = new TypeSaveRequest();
        r.setTypeCode("EVIL"); r.setTypeName("恶意"); r.setTableName("T_EVIL; DROP TABLE DICT_TYPE;--");
        var ex = assertThrows(Exception.class, () -> service.createType(r));
        assertTrue(ex.getMessage().contains("格式不合法"));
    }

    @Test @Order(12)
    void testIllegalColumnNameRejected() {
        jdbc.update("INSERT INTO DICT_TYPE (ID,TYPE_CODE,TYPE_NAME,TABLE_NAME) VALUES ('type999','TMP','临时','T_TMP')");
        FieldSaveRequest r = new FieldSaveRequest();
        r.setFieldCode("bad"); r.setFieldName("坏列"); r.setFieldType("TEXT");
        r.setColumnName("C_BAD'; DELETE FROM DICT_TYPE;--");
        var ex = assertThrows(Exception.class, () -> service.addField("TMP", r));
        assertTrue(ex.getMessage().contains("格式不合法"));
    }

    // ==================== 辅助方法 ====================

    private void crud(String typeCode, String createName, Map<String,Object> createFields,
                      String updateName, Map<String,Object> updateFields,
                      List<String> createTags, List<String> updateTags) {
        DictEntry e = service.createEntry(buildReq(typeCode, createName, createFields, createTags));
        String id = e.getId();
        assertEquals(createFields.size(), e.getFields().size());
        assertNotNull(service.getEntry(typeCode, id));

        DictEntry upd = service.updateEntry(buildUpdReq(typeCode, id, updateName, updateFields, updateTags));
        assertEquals(updateName, upd.getEntryName());

        PageResult<DictEntry> page = service.queryEntries(queryReq(typeCode));
        assertTrue(page.getTotal() >= 1);

        service.deleteEntry(typeCode, id);
        assertNotFound(typeCode, id);
    }

    private void assertNotFound(String typeCode, String id) {
        var ex = assertThrows(Exception.class, () -> service.getEntry(typeCode, id));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    private EntrySaveRequest buildReq(String typeCode, String name, Map<String,Object> fields, List<String> tags) {
        EntrySaveRequest r = new EntrySaveRequest();
        r.setTypeCode(typeCode); r.setEntryName(name); r.setFields(fields); r.setTagIds(tags);
        return r;
    }

    private EntrySaveRequest buildUpdReq(String typeCode, String id, String name, Map<String,Object> fields, List<String> tags) {
        EntrySaveRequest r = buildReq(typeCode, name, fields, tags);
        r.setId(id);
        return r;
    }

    private EntryQueryRequest queryReq(String typeCode) {
        EntryQueryRequest q = new EntryQueryRequest();
        q.setTypeCode(typeCode); q.setPageNum(1); q.setPageSize(10);
        return q;
    }

    private Map<String,Object> map2(String k1, Object v1, String k2, Object v2) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put(k1,v1); m.put(k2,v2);
        return m;
    }

    private Map<String,Object> mapN(String... kv) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0; i<kv.length; i+=2) m.put(kv[i], kv[i+1]);
        return m;
    }

    private void regType(String id, String code, String table) {
        if (count("DICT_TYPE", id) == 0)
            jdbc.update("INSERT INTO DICT_TYPE (ID,TYPE_CODE,TYPE_NAME,TABLE_NAME) VALUES (?,?,?,?)",id,code,code,table);
    }

    private void regField(String id, String typeId, String code, String type, String col, int sort, String req) {
        if (count("DICT_FIELD", id) == 0)
            jdbc.update("INSERT INTO DICT_FIELD (ID,TYPE_ID,FIELD_CODE,FIELD_NAME,FIELD_TYPE,COLUMN_NAME,SORT_ORDER,IS_REQUIRED) VALUES (?,?,?,?,?,?,?,?)",id,typeId,code,code,type,col,sort,req);
    }

    private void regTag(String id, String name) {
        if (count("DICT_TAG", id) == 0)
            jdbc.update("INSERT INTO DICT_TAG (ID,TAG_NAME) VALUES (?,?)",id,name);
    }

    private int count(String table, String id) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE ID = ?", Integer.class, id);
        return c == null ? 0 : c;
    }
}
