# 通用数据字典 — 接口文档

**Base URL:** `http://localhost:8080`
**Content-Type:** `application/json`
**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

所有接口返回统一响应格式：

```json
{
  "code": 200,         // 200-成功, 400-参数错误, 404-不存在, 409-冲突, 500-服务器错误
  "message": "success",
  "data": { ... }
}
```

---

## 1. 类型管理

### 1.1 注册字典类型

**POST** `/api/dict/type`

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| typeCode | String | 是 | 类型编码，全局唯一，如 PRODUCT |
| typeName | String | 是 | 类型显示名称，如 "产品" |
| tableName | String | 是 | 对应物理表名，如 T_PRODUCT |

```json
{
  "typeCode": "PRODUCT",
  "typeName": "产品",
  "tableName": "T_PRODUCT"
}
```

返回体 `data` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | UUID 主键 |
| typeCode | String | 类型编码 |
| typeName | String | 类型名称 |
| tableName | String | 物理表名 |
| createdAt | String | 创建时间 (ISO 8601) |
| updatedAt | String | 更新时间 (ISO 8601) |

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4e5f6",
    "typeCode": "PRODUCT",
    "typeName": "产品",
    "tableName": "T_PRODUCT",
    "createdAt": "2026-07-01T12:00:00",
    "updatedAt": "2026-07-01T12:00:00"
  }
}
```

可能的错误码：409（typeCode 重复）

---

### 1.2 修改字典类型

**PUT** `/api/dict/type`

入参与 1.1 完全一致。返回体与 1.1 一致。

可能的错误码：404（typeCode 不存在）

---

### 1.3 删除字典类型

**DELETE** `/api/dict/type/{typeCode}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 类型编码 |

无请求体。返回 `data` 为 null。

删除时会级联：清理物理表数据 → 删除条目-标签关联 → 删除字段定义 → 删除类型。

可能的错误码：404（typeCode 不存在）

---

### 1.4 查询类型详情（含字段列表）

**GET** `/api/dict/type/{typeCode}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 类型编码 |

返回体 `data` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| type | DictType | 类型基本信息（结构同 1.1） |
| fields | List\<DictField\> | 该类型下的字段定义列表 |

DictField 结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | UUID |
| typeId | String | 所属类型 ID |
| fieldCode | String | 字段编码（API 使用） |
| fieldName | String | 字段显示名 |
| fieldType | String | TEXT / NUMBER / DATE / BOOL |
| columnName | String | 物理表列名 |
| sortOrder | Integer | 排序 |
| isRequired | String | "1" 必填 / "0" 非必填 |
| createdAt | String | 创建时间 |

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "type": {
      "id": "type001",
      "typeCode": "PRODUCT",
      "typeName": "产品",
      "tableName": "T_PRODUCT",
      "createdAt": "2026-07-01T10:00:00",
      "updatedAt": "2026-07-01T10:00:00"
    },
    "fields": [
      {
        "id": "f001",
        "typeId": "type001",
        "fieldCode": "price",
        "fieldName": "价格",
        "fieldType": "NUMBER",
        "columnName": "C_PRICE",
        "sortOrder": 1,
        "isRequired": "1",
        "createdAt": "2026-07-01T10:00:00"
      },
      {
        "id": "f002",
        "typeId": "type001",
        "fieldCode": "description",
        "fieldName": "描述",
        "fieldType": "TEXT",
        "columnName": "C_DESC",
        "sortOrder": 2,
        "isRequired": "0",
        "createdAt": "2026-07-01T10:00:00"
      }
    ]
  }
}
```

---

### 1.5 查询所有类型（不含字段）

**GET** `/api/dict/type`

无入参。返回 `data` 为 `List<DictType>`。

---

## 2. 字段管理

### 2.1 添加字段定义

**POST** `/api/dict/type/{typeCode}/field`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 所属类型编码 |

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fieldCode | String | 是 | 字段编码，同一类型内唯一 |
| fieldName | String | 是 | 字段显示名 |
| fieldType | String | 是 | TEXT / NUMBER / DATE / BOOL |
| columnName | String | 是 | 对应的物理表列名 |
| sortOrder | int | 否 | 排序序号，默认 0 |
| isRequired | String | 否 | "1" 必填 / "0" 非必填，默认 "0" |

```json
{
  "fieldCode": "price",
  "fieldName": "价格",
  "fieldType": "NUMBER",
  "columnName": "C_PRICE",
  "sortOrder": 1,
  "isRequired": "1"
}
```

返回 `data` 为 DictField（结构见 1.4）。

可能错误码：404（类型不存在）、409（fieldCode 重复）

---

### 2.2 修改字段定义

**PUT** `/api/dict/type/{typeCode}/field`

入参与 2.1 完全一致。返回体与 2.1 一致。

可能错误码：404（类型或字段不存在）

---

### 2.3 删除字段定义

**DELETE** `/api/dict/type/{typeCode}/field/{fieldCode}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 所属类型编码 |
| fieldCode | 字段编码 |

无请求体。返回 `data` 为 null。

---

## 3. 条目 CRUD

### 3.1 新增条目

**POST** `/api/dict/entry`

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| typeCode | String | 是 | 类型编码 |
| entryName | String | 是 | 条目名称 |
| fields | Map\<String, Object\> | 是 | 动态字段，key 为 fieldCode，value 为字段值 |
| tagIds | List\<String\> | 否 | 关联的标签 ID 列表 |

注意：`fields` 中的 key 必须是该类型已注册的 fieldCode，否则返回 400。必填字段不得为空。NUMBER 类型字段的值可传数字或数字字符串。

```json
{
  "typeCode": "PRODUCT",
  "entryName": "iPhone 15",
  "fields": {
    "price": 6999,
    "description": "苹果手机"
  },
  "tagIds": ["tag001", "tag003"]
}
```

返回体 `data` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 条目 UUID |
| typeCode | String | 类型编码 |
| entryName | String | 条目名称 |
| fields | Map\<String, Object\> | 动态字段键值对 |
| tags | List\<DictTag\> | 关联的标签列表 |
| createdAt | String | 创建时间 |
| updatedAt | String | 更新时间 |

DictTag 结构：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 标签 UUID |
| tagName | String | 标签名称 |
| createdAt | String | 创建时间 |

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4e5f6",
    "typeCode": "PRODUCT",
    "entryName": "iPhone 15",
    "fields": {
      "price": 6999,
      "description": "苹果手机"
    },
    "tags": [
      { "id": "tag001", "tagName": "热门", "createdAt": "2026-07-01T10:00:00" },
      { "id": "tag003", "tagName": "VIP", "createdAt": "2026-07-01T10:00:00" }
    ],
    "createdAt": "2026-07-01T12:00:00",
    "updatedAt": "2026-07-01T12:00:00"
  }
}
```

可能错误码：404（类型不存在）、400（字段不合法或必填字段为空）

---

### 3.2 更新条目

**PUT** `/api/dict/entry`

入参与 3.1 相比多了 `id` 字段（必填）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 要更新的条目 ID |
| typeCode | String | 是 | 类型编码 |
| entryName | String | 是 | 条目名称 |
| fields | Map\<String, Object\> | 是 | 动态字段 |
| tagIds | List\<String\> | 否 | 标签 ID 列表（先删后插） |

```json
{
  "id": "a1b2c3d4e5f6",
  "typeCode": "PRODUCT",
  "entryName": "iPhone 15 Pro",
  "fields": {
    "price": 7999,
    "description": "苹果旗舰手机"
  },
  "tagIds": ["tag001"]
}
```

返回体与 3.1 一致。

标签关联采用先删后插策略，所以 `tagIds` 传什么最终就只有什么标签。

可能错误码：400（id 为空）、404（类型或条目不存在）

---

### 3.3 删除条目

**DELETE** `/api/dict/entry/{typeCode}/{id}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 类型编码 |
| id | 条目 ID |

无请求体。返回 `data` 为 null。

删除时会同时删掉该条目的所有标签关联。

---

### 3.4 查询单个条目

**GET** `/api/dict/entry/{typeCode}/{id}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 类型编码 |
| id | 条目 ID |

返回体与 3.1 一致。可能错误码：404（类型或条目不存在）

---

### 3.5 分页查询条目列表

**POST** `/api/dict/entry/query`

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| typeCode | String | 是 | 类型编码 |
| tagIds | List\<String\> | 否 | 标签 ID 列表，多个取交集，为空则不过滤 |
| pageNum | int | 否 | 页码，从 1 开始，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |

```json
{
  "typeCode": "PRODUCT",
  "tagIds": ["tag001"],
  "pageNum": 1,
  "pageSize": 10
}
```

返回体 `data` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| total | long | 总记录数 |
| pageNum | int | 当前页码 |
| pageSize | int | 每页条数 |
| records | List\<DictEntry\> | 条目列表（结构同 3.1） |

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 2,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": "p002",
        "typeCode": "PRODUCT",
        "entryName": "华为 Mate 60",
        "fields": { "price": 5999, "description": "华为旗舰" },
        "tags": [{ "id": "tag001", "tagName": "热门", "createdAt": "2026-07-01T10:00:00" }],
        "createdAt": "2026-07-01T10:00:00",
        "updatedAt": "2026-07-01T10:00:00"
      },
      {
        "id": "p001",
        "typeCode": "PRODUCT",
        "entryName": "iPhone 15",
        "fields": { "price": 6999, "description": "苹果手机" },
        "tags": [
          { "id": "tag001", "tagName": "热门", "createdAt": "2026-07-01T10:00:00" },
          { "id": "tag003", "tagName": "VIP", "createdAt": "2026-07-01T10:00:00" }
        ],
        "createdAt": "2026-07-01T10:00:00",
        "updatedAt": "2026-07-01T10:00:00"
      }
    ]
  }
}
```

标签过滤逻辑：传入 `tagIds: ["tag001", "tag003"]` 时，返回**同时拥有** tag001 和 tag003 的条目（交集）。

---

## 4. 标签管理

### 4.1 创建标签

**POST** `/api/dict/tag`

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tagName | String | 是 | 标签名，全局唯一 |

```json
{
  "tagName": "热门"
}
```

返回 `data` 为 DictTag（结构见 3.1）。

可能错误码：409（tagName 重复）

---

### 4.2 修改标签

**PUT** `/api/dict/tag`

请求体：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 标签 ID |
| tagName | String | 是 | 新标签名 |

```json
{
  "id": "tag001",
  "tagName": "超级热门"
}
```

返回体与 4.1 一致。

可能错误码：404（标签不存在）

---

### 4.3 删除标签

**DELETE** `/api/dict/tag/{id}`

| 路径参数 | 说明 |
|----------|------|
| id | 标签 ID |

无请求体。返回 `data` 为 null。

---

### 4.4 查询所有标签

**GET** `/api/dict/tag`

无入参。返回 `data` 为 `List<DictTag>`。

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": "tag001", "tagName": "热门", "createdAt": "2026-07-01T10:00:00" },
    { "id": "tag002", "tagName": "冷门", "createdAt": "2026-07-01T10:00:00" },
    { "id": "tag003", "tagName": "VIP", "createdAt": "2026-07-01T10:00:00" }
  ]
}
```

---

### 4.5 查询某类型下的标签

**GET** `/api/dict/tag/{typeCode}`

| 路径参数 | 说明 |
|----------|------|
| typeCode | 类型编码 |

返回该类型下所有条目关联过的标签（去重）。结构与 4.4 一致。

---

## 5. 错误码汇总

| HTTP 状态码 | code | 场景 |
|-------------|------|------|
| 400 | 400 | 参数校验失败、未知字段、必填字段为空、类型校验失败 |
| 404 | 404 | 类型不存在、字段不存在、条目不存在、标签不存在 |
| 409 | 409 | typeCode 重复、fieldCode 重复、tagName 重复 |
| 500 | 500 | 服务器内部错误 |

## 6. 完整使用示例

以下是从零开始创建一个新字典类型并操作数据的完整 curl 示例：

```bash
# 1. 注册类型
curl -X POST http://localhost:8080/api/dict/type \
  -H "Content-Type: application/json" \
  -d '{"typeCode":"PRODUCT","typeName":"产品","tableName":"T_PRODUCT"}'

# 2. 添加字段
curl -X POST http://localhost:8080/api/dict/type/PRODUCT/field \
  -H "Content-Type: application/json" \
  -d '{"fieldCode":"price","fieldName":"价格","fieldType":"NUMBER","columnName":"C_PRICE","sortOrder":1,"isRequired":"1"}'

curl -X POST http://localhost:8080/api/dict/type/PRODUCT/field \
  -H "Content-Type: application/json" \
  -d '{"fieldCode":"description","fieldName":"描述","fieldType":"TEXT","columnName":"C_DESC","sortOrder":2,"isRequired":"0"}'

# 3. 创建标签
curl -X POST http://localhost:8080/api/dict/tag \
  -H "Content-Type: application/json" \
  -d '{"tagName":"热门"}'

# 4. 新增条目
curl -X POST http://localhost:8080/api/dict/entry \
  -H "Content-Type: application/json" \
  -d '{"typeCode":"PRODUCT","entryName":"iPhone 15","fields":{"price":6999,"description":"苹果手机"},"tagIds":["tag001"]}'

# 5. 查询列表（按标签过滤）
curl -X POST http://localhost:8080/api/dict/entry/query \
  -H "Content-Type: application/json" \
  -d '{"typeCode":"PRODUCT","tagIds":["tag001"],"pageNum":1,"pageSize":10}'
```
