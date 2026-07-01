-- ============================================================
-- 数据字典 Demo — MySQL 初始化脚本
-- 主键统一使用 UUID（32 位字符串）
-- 请先手动创建数据库：CREATE DATABASE IF NOT EXISTS data_dict DEFAULT CHARACTER SET utf8mb4;
-- ============================================================

-- 元数据层：类型定义
CREATE TABLE IF NOT EXISTS DICT_TYPE (
    ID          VARCHAR(32) PRIMARY KEY,
    TYPE_CODE   VARCHAR(64) NOT NULL UNIQUE,
    TYPE_NAME   VARCHAR(128) NOT NULL,
    TABLE_NAME  VARCHAR(64) NOT NULL,
    CREATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 元数据层：字段定义
CREATE TABLE IF NOT EXISTS DICT_FIELD (
    ID          VARCHAR(32) PRIMARY KEY,
    TYPE_ID     VARCHAR(32) NOT NULL,
    FIELD_CODE  VARCHAR(64) NOT NULL,
    FIELD_NAME  VARCHAR(128) NOT NULL,
    FIELD_TYPE  VARCHAR(32) NOT NULL,
    COLUMN_NAME VARCHAR(64) NOT NULL,
    SORT_ORDER  INT DEFAULT 0,
    IS_REQUIRED CHAR(1) DEFAULT '0',
    CREATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID)
);

-- 标签层
CREATE TABLE IF NOT EXISTS DICT_TAG (
    ID          VARCHAR(32) PRIMARY KEY,
    TAG_NAME    VARCHAR(64) NOT NULL UNIQUE,
    CREATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 条目-标签关联
CREATE TABLE IF NOT EXISTS DICT_ENTRY_TAG (
    ID          VARCHAR(32) PRIMARY KEY,
    TYPE_ID     VARCHAR(32) NOT NULL,
    ENTRY_ID    VARCHAR(32) NOT NULL,
    TAG_ID      VARCHAR(32) NOT NULL,
    FOREIGN KEY (TYPE_ID) REFERENCES DICT_TYPE(ID),
    FOREIGN KEY (TAG_ID) REFERENCES DICT_TAG(ID),
    UNIQUE (TYPE_ID, ENTRY_ID, TAG_ID)
);

-- ============================================================
-- 物理表（7 种类型，字段数从 2 到 12，差异明显）
-- ============================================================

-- 1. 产品字典（2 个业务字段）
CREATE TABLE IF NOT EXISTS T_PRODUCT (
    ID          VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME  VARCHAR(256) NOT NULL,
    C_PRICE     DECIMAL(18,4),
    C_DESC      VARCHAR(4000),
    CREATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 客户字典（7 个业务字段，TEXT/NUMBER/DATE/BOOL）
CREATE TABLE IF NOT EXISTS T_CUSTOMER (
    ID             VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME     VARCHAR(256) NOT NULL,
    C_PHONE        VARCHAR(20),
    C_EMAIL        VARCHAR(128),
    C_COMPANY      VARCHAR(256),
    C_AGE          INT,
    C_VIP_LEVEL    INT,
    C_BIRTHDAY     DATE,
    C_IS_ACTIVE    CHAR(1),
    CREATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 合同字典（8 个业务字段）
CREATE TABLE IF NOT EXISTS T_CONTRACT (
    ID             VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME     VARCHAR(256) NOT NULL,
    C_PARTY_A      VARCHAR(256),
    C_PARTY_B      VARCHAR(256),
    C_AMOUNT       DECIMAL(18,2),
    C_SIGN_DATE    DATE,
    C_EXPIRE_DATE  DATE,
    C_IS_SEALED    CHAR(1),
    C_FILE_URL     VARCHAR(512),
    C_REMARK       VARCHAR(4000),
    CREATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. FIELD_OPTION（6 个业务字段，字段可选值管理）
CREATE TABLE IF NOT EXISTS T_FIELD_OPTION (
    ID             VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME     VARCHAR(256) NOT NULL,
    C_FIELD_ID     VARCHAR(64),
    C_FIELD_NAME   VARCHAR(128),
    C_VALUE        VARCHAR(256),
    C_LABEL        VARCHAR(256),
    C_DESCRIPTION  VARCHAR(1000),
    C_SORT_ORDER   INT,
    C_IS_ACTIVE    CHAR(1),
    CREATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 5. FIELD_MAPPING（5 个业务字段，Jira 多站点字段 ID 映射）
CREATE TABLE IF NOT EXISTS T_FIELD_MAPPING (
    ID                VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME        VARCHAR(256) NOT NULL,
    C_SITE_ID         VARCHAR(64),
    C_LOCAL_FIELD     VARCHAR(128),
    C_REMOTE_FIELD_ID VARCHAR(128),
    C_REMOTE_FIELD_NAME VARCHAR(256),
    C_FIELD_TYPE      VARCHAR(64),
    CREATED_AT        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 6. APP_CODE（10 个业务字段，应用代码管理）
CREATE TABLE IF NOT EXISTS T_APP_CODE (
    ID             VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME     VARCHAR(256) NOT NULL,
    C_APP_CODE     VARCHAR(64),
    C_APP_NAME     VARCHAR(128),
    C_DESCRIPTION  VARCHAR(1000),
    C_OWNER        VARCHAR(64),
    C_TEAM         VARCHAR(128),
    C_LANGUAGE     VARCHAR(32),
    C_REPO_URL     VARCHAR(512),
    C_DEPLOY_ENV   VARCHAR(64),
    C_STATUS       VARCHAR(32),
    C_RELEASE_DATE DATE,
    CREATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 7. CLA_PROJECT（12 个业务字段，CLA 项目管理）
CREATE TABLE IF NOT EXISTS T_CLA_PROJECT (
    ID               VARCHAR(32) PRIMARY KEY,
    ENTRY_NAME       VARCHAR(256) NOT NULL,
    C_PROJECT_CODE   VARCHAR(64),
    C_ORGANIZATION   VARCHAR(256),
    C_MAINTAINER     VARCHAR(64),
    C_LICENSE        VARCHAR(128),
    C_REPO_URL       VARCHAR(512),
    C_SIGN_DATE      DATE,
    C_EXPIRE_DATE    DATE,
    C_APPROVAL_STATUS VARCHAR(32),
    C_CLA_TYPE       VARCHAR(32),
    C_SIGNATORY_COUNT INT,
    C_REMARK         VARCHAR(4000),
    CREATED_AT       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- 清理旧数据（幂等）
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM DICT_ENTRY_TAG WHERE 1=1;
DELETE FROM T_PRODUCT WHERE 1=1;
DELETE FROM T_CUSTOMER WHERE 1=1;
DELETE FROM T_CONTRACT WHERE 1=1;
DELETE FROM T_FIELD_OPTION WHERE 1=1;
DELETE FROM T_FIELD_MAPPING WHERE 1=1;
DELETE FROM T_APP_CODE WHERE 1=1;
DELETE FROM T_CLA_PROJECT WHERE 1=1;
DELETE FROM DICT_TAG WHERE 1=1;
DELETE FROM DICT_FIELD WHERE 1=1;
DELETE FROM DICT_TYPE WHERE 1=1;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 注册 7 种类型
-- ============================================================
INSERT INTO DICT_TYPE (ID, TYPE_CODE, TYPE_NAME, TABLE_NAME) VALUES
('type001', 'PRODUCT',       '产品',          'T_PRODUCT'),
('type002', 'CUSTOMER',      '客户',          'T_CUSTOMER'),
('type003', 'CONTRACT',      '合同',          'T_CONTRACT'),
('type004', 'FIELD_OPTION',  '字段选项',      'T_FIELD_OPTION'),
('type005', 'FIELD_MAPPING', 'Jira字段映射',  'T_FIELD_MAPPING'),
('type006', 'APP_CODE',      '应用代码',      'T_APP_CODE'),
('type007', 'CLA_PROJECT',   'CLA项目',       'T_CLA_PROJECT');

-- ============================================================
-- 注册字段定义（按类型，字段数：2/7/8/7/5/10/12）
-- ============================================================

-- PRODUCT: 2 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f001', 'type001', 'price',       '价格', 'NUMBER', 'C_PRICE', 1, '1'),
('f002', 'type001', 'description', '描述', 'TEXT',   'C_DESC',  2, '0');

-- CUSTOMER: 7 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f003', 'type002', 'phone',      '手机号',   'TEXT',   'C_PHONE',     1, '1'),
('f004', 'type002', 'email',      '邮箱',     'TEXT',   'C_EMAIL',     2, '0'),
('f005', 'type002', 'company',    '公司',     'TEXT',   'C_COMPANY',   3, '0'),
('f006', 'type002', 'age',        '年龄',     'NUMBER', 'C_AGE',       4, '0'),
('f007', 'type002', 'vipLevel',   'VIP等级',  'NUMBER', 'C_VIP_LEVEL', 5, '0'),
('f008', 'type002', 'birthday',   '生日',     'DATE',   'C_BIRTHDAY',  6, '0'),
('f009', 'type002', 'isActive',   '是否活跃', 'BOOL',   'C_IS_ACTIVE', 7, '1');

-- CONTRACT: 8 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f010', 'type003', 'partyA',     '甲方',     'TEXT',   'C_PARTY_A',     1, '1'),
('f011', 'type003', 'partyB',     '乙方',     'TEXT',   'C_PARTY_B',     2, '1'),
('f012', 'type003', 'amount',     '合同金额', 'NUMBER', 'C_AMOUNT',      3, '1'),
('f013', 'type003', 'signDate',   '签订日期', 'DATE',   'C_SIGN_DATE',   4, '1'),
('f014', 'type003', 'expireDate', '到期日期', 'DATE',   'C_EXPIRE_DATE', 5, '0'),
('f015', 'type003', 'isSealed',   '是否盖章', 'BOOL',   'C_IS_SEALED',   6, '0'),
('f016', 'type003', 'fileUrl',    '文件链接', 'TEXT',   'C_FILE_URL',    7, '0'),
('f017', 'type003', 'remark',     '备注',     'TEXT',   'C_REMARK',      8, '0');

-- FIELD_OPTION: 7 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f018', 'type004', 'fieldId',     '关联字段ID', 'TEXT',   'C_FIELD_ID',    1, '1'),
('f019', 'type004', 'fieldName',   '关联字段名', 'TEXT',   'C_FIELD_NAME',  2, '0'),
('f020', 'type004', 'label',       '选项标签',   'TEXT',   'C_LABEL',       3, '1'),
('f021', 'type004', 'value',       '选项值',     'TEXT',   'C_VALUE',       4, '1'),
('f022', 'type004', 'description', '描述',       'TEXT',   'C_DESCRIPTION', 5, '0'),
('f023', 'type004', 'sortOrder',   '排序',       'NUMBER', 'C_SORT_ORDER',  6, '0'),
('f024', 'type004', 'isActive',    '是否启用',   'BOOL',   'C_IS_ACTIVE',   7, '1');

-- FIELD_MAPPING: 5 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f025', 'type005', 'siteId',          'Jira站点ID',      'TEXT', 'C_SITE_ID',          1, '1'),
('f026', 'type005', 'localField',      '本地字段名',      'TEXT', 'C_LOCAL_FIELD',      2, '1'),
('f027', 'type005', 'remoteFieldId',   '远程字段ID',      'TEXT', 'C_REMOTE_FIELD_ID',  3, '1'),
('f028', 'type005', 'remoteFieldName', '远程字段名',      'TEXT', 'C_REMOTE_FIELD_NAME',4, '0'),
('f029', 'type005', 'fieldType',       '字段类型',        'TEXT', 'C_FIELD_TYPE',       5, '0');

-- APP_CODE: 10 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f030', 'type006', 'appCode',     '应用编码', 'TEXT',   'C_APP_CODE',     1, '1'),
('f031', 'type006', 'appName',     '应用名称', 'TEXT',   'C_APP_NAME',     2, '1'),
('f032', 'type006', 'description', '描述',     'TEXT',   'C_DESCRIPTION',  3, '0'),
('f033', 'type006', 'owner',       '负责人',   'TEXT',   'C_OWNER',        4, '1'),
('f034', 'type006', 'team',        '所属团队', 'TEXT',   'C_TEAM',         5, '0'),
('f035', 'type006', 'language',    '开发语言', 'TEXT',   'C_LANGUAGE',     6, '0'),
('f036', 'type006', 'repoUrl',     '代码仓库', 'TEXT',   'C_REPO_URL',     7, '0'),
('f037', 'type006', 'deployEnv',   '部署环境', 'TEXT',   'C_DEPLOY_ENV',   8, '0'),
('f038', 'type006', 'status',      '状态',     'TEXT',   'C_STATUS',       9, '1'),
('f039', 'type006', 'releaseDate', '上线日期', 'DATE',   'C_RELEASE_DATE', 10, '0');

-- CLA_PROJECT: 12 个字段
INSERT INTO DICT_FIELD (ID, TYPE_ID, FIELD_CODE, FIELD_NAME, FIELD_TYPE, COLUMN_NAME, SORT_ORDER, IS_REQUIRED) VALUES
('f040', 'type007', 'projectCode',     '项目编码',   'TEXT',   'C_PROJECT_CODE',     1, '1'),
('f041', 'type007', 'organization',    '所属组织',   'TEXT',   'C_ORGANIZATION',     2, '1'),
('f042', 'type007', 'maintainer',      '维护人',     'TEXT',   'C_MAINTAINER',       3, '1'),
('f043', 'type007', 'license',         '开源协议',   'TEXT',   'C_LICENSE',          4, '0'),
('f044', 'type007', 'repoUrl',         '代码仓库',   'TEXT',   'C_REPO_URL',         5, '0'),
('f045', 'type007', 'signDate',        '签署日期',   'DATE',   'C_SIGN_DATE',        6, '0'),
('f046', 'type007', 'expireDate',      '到期日期',   'DATE',   'C_EXPIRE_DATE',      7, '0'),
('f047', 'type007', 'approvalStatus',  '审批状态',   'TEXT',   'C_APPROVAL_STATUS',  8, '1'),
('f048', 'type007', 'claType',         'CLA类型',    'TEXT',   'C_CLA_TYPE',          9, '1'),
('f049', 'type007', 'signatoryCount',  '签署人数',   'NUMBER', 'C_SIGNATORY_COUNT',  10, '0'),
('f050', 'type007', 'remark',          '备注',       'TEXT',   'C_REMARK',           11, '0');

-- ============================================================
-- 标签
-- ============================================================
INSERT INTO DICT_TAG (ID, TAG_NAME) VALUES
('tag001', '热门'),
('tag002', '冷门'),
('tag003', 'VIP'),
('tag004', '重要'),
('tag005', '已过期');

-- ============================================================
-- 示例数据
-- ============================================================

-- PRODUCT（2 字段）
INSERT INTO T_PRODUCT (ID, ENTRY_NAME, C_PRICE, C_DESC) VALUES
('p001', 'iPhone 15',     6999, '苹果手机'),
('p002', '华为 Mate 60',  5999, '华为旗舰'),
('p003', '小米 14',       3999, '小米年度旗舰');

-- CUSTOMER（7 字段）
INSERT INTO T_CUSTOMER (ID, ENTRY_NAME, C_PHONE, C_EMAIL, C_COMPANY, C_AGE, C_VIP_LEVEL, C_BIRTHDAY, C_IS_ACTIVE) VALUES
('c001', '张三', '13800138000', 'zhangsan@example.com',   '阿里巴巴', 28, 3, '1998-05-15', '1'),
('c002', '李四', '13900139000', 'lisi@example.com',       '腾讯',     35, 5, '1991-08-20', '1'),
('c003', '王五', '13700137000', 'wangwu@example.com',     '字节跳动', 42, 2, '1984-11-03', '0');

-- CONTRACT（8 字段）
INSERT INTO T_CONTRACT (ID, ENTRY_NAME, C_PARTY_A, C_PARTY_B, C_AMOUNT, C_SIGN_DATE, C_EXPIRE_DATE, C_IS_SEALED, C_FILE_URL, C_REMARK) VALUES
('ct001', '年度采购协议', '甲方公司A', '乙方公司B', 500000.00, '2026-01-15', '2026-12-31', '1', 'https://oss.example.com/file/ct001.pdf', '已完成盖章归档'),
('ct002', '技术服务合同', '甲方公司C', '乙方公司D', 120000.00, '2026-03-01', '2027-02-28', '0', NULL,                                      '等待双方盖章');

-- FIELD_OPTION（7 字段，优先级和状态两个字段的选项值）
INSERT INTO T_FIELD_OPTION (ID, ENTRY_NAME, C_FIELD_ID, C_FIELD_NAME, C_VALUE, C_LABEL, C_DESCRIPTION, C_SORT_ORDER, C_IS_ACTIVE) VALUES
('fo001', '优先级-High',   'customfield_10001', 'priority', 'high',   '高', '紧急处理',         1, '1'),
('fo002', '优先级-Medium', 'customfield_10001', 'priority', 'medium', '中', '正常处理',         2, '1'),
('fo003', '优先级-Low',    'customfield_10001', 'priority', 'low',    '低', '可延后处理',       3, '1'),
('fo004', '状态-Open',     'customfield_10002', 'status',   'open',   '打开', '创建后默认状态', 1, '1'),
('fo005', '状态-Closed',   'customfield_10002', 'status',   'closed', '关闭', '终态',            2, '1');

-- FIELD_MAPPING（5 字段，同一字段在两个 Jira 站点的 ID 映射）
INSERT INTO T_FIELD_MAPPING (ID, ENTRY_NAME, C_SITE_ID, C_LOCAL_FIELD, C_REMOTE_FIELD_ID, C_REMOTE_FIELD_NAME, C_FIELD_TYPE) VALUES
('fm001', '优先级-JiraProd映射',   'jira-prod',  'priority', 'customfield_10001', 'Priority', 'select'),
('fm002', '状态-JiraProd映射',     'jira-prod',  'status',   'customfield_10002', 'Status',   'select'),
('fm003', '优先级-JiraTest映射',   'jira-test',  'priority', 'customfield_20005', '优先级',   'select'),
('fm004', '状态-JiraTest映射',     'jira-test',  'status',   'customfield_20008', '状态',     'select');

-- APP_CODE（10 字段）
INSERT INTO T_APP_CODE (ID, ENTRY_NAME, C_APP_CODE, C_APP_NAME, C_DESCRIPTION, C_OWNER, C_TEAM, C_LANGUAGE, C_REPO_URL, C_DEPLOY_ENV, C_STATUS, C_RELEASE_DATE) VALUES
('ac001', '用户服务',     'USER-SERVICE',   '用户服务',   '统一用户管理',   '张三', '基础架构组', 'Java',   'https://git.example.com/user-svc',  'K8s', '已上线', '2025-03-15'),
('ac002', '网关服务',     'GATEWAY',        'API网关',   '流量入口管理',   '李四', '基础架构组', 'Go',     'https://git.example.com/gateway',    'K8s', '已上线', '2025-06-01'),
('ac003', '数据分析平台', 'DATA-ANALYTICS', '数据平台', '数据报表与分析', '王五', '数据组',     'Python', 'https://git.example.com/data-plat', 'ECS', '开发中', NULL);

-- CLA_PROJECT（12 字段）
INSERT INTO T_CLA_PROJECT (ID, ENTRY_NAME, C_PROJECT_CODE, C_ORGANIZATION, C_MAINTAINER, C_LICENSE, C_REPO_URL, C_SIGN_DATE, C_EXPIRE_DATE, C_APPROVAL_STATUS, C_CLA_TYPE, C_SIGNATORY_COUNT, C_REMARK) VALUES
('cp001', '开源项目Sky',     'SKY-CLA',     'Apache', '张三', 'Apache 2.0', 'https://git.example.com/sky',      '2025-01-10', '2026-01-10', '已通过', '企业CLA', 45,  '年度续签完成'),
('cp002', '开源项目Ocean',   'OCEAN-CLA',   'CNCF',   '李四', 'MIT',        'https://git.example.com/ocean',    '2025-06-20', '2026-06-20', '已通过', '个人CLA', 120, NULL),
('cp003', '待审批项目River', 'RIVER-CLA',   'Eclipse','王五', 'EPL-2.0',    'https://git.example.com/river',    NULL,          NULL,        '待审批', '企业CLA', 0,    '等待法务审核');

-- ============================================================
-- 条目-标签关联
-- ============================================================
INSERT INTO DICT_ENTRY_TAG (ID, TYPE_ID, ENTRY_ID, TAG_ID) VALUES
('et01', 'type001', 'p001', 'tag001'),
('et02', 'type001', 'p001', 'tag003'),
('et03', 'type001', 'p002', 'tag001'),
('et04', 'type002', 'c001', 'tag003'),
('et05', 'type002', 'c001', 'tag004'),
('et06', 'type002', 'c002', 'tag004'),
('et07', 'type003', 'ct001', 'tag004'),
('et08', 'type003', 'ct002', 'tag002'),
('et09', 'type004', 'fo001', 'tag001'),
('et10', 'type005', 'fm001', 'tag004'),
('et11', 'type006', 'ac001', 'tag001'),
('et12', 'type006', 'ac001', 'tag004'),
('et13', 'type007', 'cp001', 'tag004'),
('et14', 'type007', 'cp003', 'tag002'),
('et15', 'type007', 'cp003', 'tag005');
