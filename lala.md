

# 第一层：代码级债务（最直观）

**表现（你已有）**：命名混乱、重复代码、超长方法、硬编码
**影响（你已有）**：可读性差，改一处动全身

## 额外场景举例

* 同样的“校验逻辑”在多个 Controller/Service 里复制粘贴，改一个规则要改 N 处。
* 魔法数字（magic number）散落：状态码、时间窗口、阈值等直接写在 if/else 中。
* 方法过长：一个方法既做参数校验、DB 操作、业务逻辑、转换、外部调用，职责不清。

## Java 示例 — 坏例子

```java
// Bad: magic numbers, duplicated logic, 超长方法（伪代码）
public class OrderService {
    public void process(Order o) {
        // validate
        if (o.getAmount() <= 0 || o.getItems() == null) {
            throw new IllegalArgumentException("invalid");
        }
        // discount logic duplicated across codebase
        double discount = 0;
        if (o.getCustomerType().equals("VIP")) {
            discount = o.getAmount() * 0.1; // magic number 0.1
        }
        // compute final amount in many places...
        double finalAmount = o.getAmount() - discount;
        // many more lines: DB save, notify, logging...
    }
}
```

## Java 示例 — 改进

```java
public class OrderService {
    private static final double VIP_DISCOUNT_RATE = 0.10;

    public void process(Order o) {
        validate(o);
        double discount = DiscountPolicy.computeDiscount(o);
        double finalAmount = o.getAmount() - discount;
        saveOrder(o, finalAmount);
        notifyIfNeeded(o);
    }

    private void validate(Order o) {
        if (o.getAmount() <= 0 || o.getItems() == null || o.getItems().isEmpty()) {
            throw new IllegalArgumentException("invalid order");
        }
    }
}
```

（把重复逻辑抽成 `DiscountPolicy`，常量集中管理，方法拆分，小函数单一职责）

## 检测信号

* PR 评论里经常出现“为什么这里又重复实现？”
* 静态分析显示大量 code smell（如 Sonar 的 duplicated blocks）
* 新人读代码耗时长、理解一次变更要跨文件查找

## 缓解建议

* 强制使用 lint/静态检查（FindBugs/SpotBugs、PMD、Sonar）并在 CI 中阻断重复率过高的提交。
* 设立代码复用/常量库（例如 `Constants` / `util`），并写 PR 模板要求“有没有重复实现？”
* 推行小函数 & 单一职责，代码 review 把“单一职责”作为硬性项。

---

# 第二层：设计级债务（隐形枷锁）

**表现**：紧耦合、弱内聚、违反 SOLID 原则
**影响**：系统难以适应变化

## 场景举例

* Service 类承担太多职能（把 DB、外部 API、缓存、消息处理都塞一块）。
* 领域边界不清，多个 team 修改同一类逻辑，会互相影响。
* 接口设计膨胀：一个 DTO 含 20+ 字段，任何新增需求都造成向后兼容修改。

## Java 示例 — 坏例子（一个巨型 Service）

```java
public class UserService {
    // 5000 行示意
    public void register(UserDto dto) {
        // 校验大量逻辑
        // create user in DB
        // create profile
        // sync to legacy system
        // send welcome email
        // update cache
        // audit logging
        // business rules for different regions
        // ...
    }
    public void updateUser(...) { /* more logic */ }
    public void deleteUser(...) { /* more logic */ }
    // many helper methods
}
```

## Java 示例 — 改进（分层与策略）

```java
public class UserRegistrationService {
    private final UserRepository repo;
    private final LegacySyncService legacySync;
    private final EmailService emailService;
    private final CacheService cacheService;

    public void register(UserDto dto) {
        validate(dto);
        User u = createUser(dto);
        legacySync.sync(u);
        emailService.sendWelcome(u);
        cacheService.evictUserList();
    }
    // 每个职责委派给明确组件
}
```

## 检测信号

* 系统改动时需要同时通知多个团队/模块。
* 一个功能改动导致连环回归测试失败。
* PR 涉及的文件过多（high churn）。

## 缓解建议

* 采用 `Bounded Context`、明确领域边界；把大型 Service 拆成小、职责清晰的组件。
* 设计 API 时坚持最小暴露和稳定契约（接口层与实现分离）。
* 通过架构评审（Architecture Decision Records）记录大的设计妥协和补偿计划。

---

# 第三层：架构级债务（代价最高）

**表现**：选错技术栈、服务拆错、数据库设计糟糕
**影响**：从根本上限制扩展性

## 场景举例

* 系统本应异步解耦（消息队列），却用同步 RPC，导致高并发下连锁超时。
* 微服务划分错误：高耦合模块被拆成多个 service，频繁跨服务调用导致延迟爆表。
* 数据库设计不考虑历史演进：大量 nullable 列、无版本化，做 schema 迁移极其昂贵。

## Java/架构 示例 — 坏例子（同步替代异步）

```text
// 原设计应使用消息队列：OrderCreated -> InventoryReserved -> Shipping
// 实际实现：同步 HTTP 调用链： OrderService -> InventoryService -> ShippingService
// 结果：下游慢导致 upstream 请求超时、线程池耗尽、回压扩散
```

## 改进策略（架构级）

* 用异步消息（Kafka/RabbitMQ）断开紧耦合，确保下游不可用时上游能退避/重试。
* 对于高调用频率的边界，合并服务或引入 API Gateway 以减少往返。
* 数据库采用版本化迁移模式（backfill、双写、feature flag 逐步切换）。

## Java 示例 — 改进思路（pseudo）

```text
// 改为：OrderService publish OrderCreated event to Kafka
// InventoryService subscribes and reserves inventory asynchronously
// OrderService reacts to reservation result via event or callback
```

## 检测信号

* 在高负载下出现链式失败（cascading failure）。
* 延迟与错误率随着调用链长度成正比上升。
* 扩容成本极高（加机器不能解决根源）。

## 缓解建议

* 做体系级的负载与调用链跟踪（APM: zipkin/jaeger，或云厂商 tracing）找瓶颈。
* 做架构演进评估（cost/benefit），优先处理会直接解除大规模痛点的改造（例如消息化关键路径）。
* 把数据库设计改为“兼容演进”策略（add-column + backfill + cutover）。

---

# 第四层：测试级债务（定时炸弹）

**表现**：没有自动化测试、覆盖率低、测试用例无效
**影响**：每次修改都像走钢丝

## 场景举例

* Controller 有集成测试，但 Service/Repository 层没单元测试，很多逻辑只有在生产回归时暴露。
* 测试依赖真实数据库或外部服务，没有 mock/isolation，CI 慢且不稳定。
* 测试断言不够严格，只检查 200 OK 而不验证业务状态或 DB 变更。

## Java 示例 — 坏例子（无效测试）

```java
// Bad test: 只验证 HTTP 200，没有验证 DB 状态或业务结果
@Test
public void testCreateOrder() throws Exception {
    mockMvc.perform(post("/orders")
        .content("{...}"))
        .andExpect(status().isOk());
    // 没有验证 DB 插入、库存更新等
}
```

## Java 示例 — 改进（单元+mock）

```java
@RunWith(MockitoJUnitRunner.class)
public class OrderServiceTest {
    @Mock OrderRepository repo;
    @Mock InventoryClient inventory;
    @InjectMocks OrderService service;

    @Test
    public void shouldReserveInventoryAndSaveOrder() {
        when(inventory.reserve(any())).thenReturn(true);
        OrderDto dto = sampleOrder();
        service.process(dto);
        verify(inventory).reserve(any());
        verify(repo).save(any(Order.class));
    }
}
```

## 检测信号

* CI 绿灯率低，flaky test 多。
* 开发本地跑测试速度极慢（数据库依赖）。
* 回归缺陷多数是“业务逻辑没有被单元测试覆盖”。

## 缓解建议

* 强制 CI 执行单元/集成测试，逐步把慢测试隔离到 nightly pipeline。
* 使用契约测试（Pact）或 contract testing 保护跨服务边界。
* 以风险为导向补充测试（优先为高变更/高价值路径写单元与集成测试）。

---

# 第五层：文档级债务（沟通障碍）

**表现**：文档缺失、过时、与代码不符
**影响**：新人上手慢，知识孤岛

## 场景举例

* API 文档（Swagger/OpenAPI）未及时更新，前端以旧契约呼叫，导致线上问题。
* 设计决策未形成 ADR（Architecture Decision Record），新成员无法理解为什么做出某些妥协。
* On-call 文档不齐，运维无法快速定位问题，造成长时间 SRE 处理。

## Java 示例 — 坏例子（过时的 Javadoc）

```java
/**
 * createUser - creates a user with name and email.
 * (旧文档：方法签名曾经是 createUser(String name, String email))
 */
public User createUser(UserDto dto) {
    // 现在参数里有更多字段，Javadoc 没更新
}
```

## 改进示例（实践）

* 使用 OpenAPI/Swagger 自动从 Controller 注解生成文档，并把生成流程纳入 CI，使 API 变更必须伴随文档变更。
* 建立 ADR 模板并在大改动时强制产出。
* 建立 On-call runbook，包含故障排查步骤与常见命令。

## 检测信号

* 新人 ramp-up 时间长（从 2 周到 2 个月）
* 频繁有人问“为什么这么做”且答案不一致
* 线上问题的排查靠某位资深工程师记忆而非文档

## 缓解建议

* 把文档更新作为 Definition of Done 的一部分（代码改动必须带文档/接口变更说明）。
* 定期做文档健检（每季度），并将过时文档标记为 archival。
* 建立“知识访问者”机制：每次重要变更都安排 15 分钟的知识传递会议并录制。

---

# 总结（给 SM / PM 的快速指导）

* 这五个维度是从**易见到难见、从低成本到高成本**排列的：代码级→设计级→架构级→测试级→文档级。
* 处理优先级通常按“影响范围 × 改善成本”来评估：优先解决能解多个痛点的项（例如：设计级问题比单个重复代码更能提升长期速度）。
* 给 SM 的可执行动作：

  1. 要求每个 major change 附带「债务评估」：影响范围、偿还计划、风险等级。
  2. 在 Sprint 中预留 10%（或按项目情况调整）的 capacity 专门用于债务偿还（先从代码级和测试级着手）。
  3. 把 doc + tests + small refactors 列为 DoD（Definition of Done）的一部分，确保变更不再创建新债务。

