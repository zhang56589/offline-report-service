# Report Service

Spring Boot 报表异步导出后端服务。异步生成 Excel、定时统计、超时检测、过期文件清理。

---

## 目录

- [技术债登记表（待优化）](#技术债登记表待优化)
- [Bug 修复历史（Changelog）](#bug-修复历史changelog)
- [技术栈 & 依赖](#技术栈--依赖)
- [目录结构](#目录结构)
- [配置参考](#配置参考)
- [数据库表结构](#数据库表结构)
- [实体 ↔ 表字段映射](#实体--表字段映射)
- [核心业务规则](#核心业务规则)
- [后端接口清单](#后端接口清单)
- [类速查表](#类速查表)
- [常用命令](#常用命令)
- [扩展指南](#扩展指南)
- [开发约定](#开发约定)
- [开发协作规范](#开发协作规范)
- [故障排查](#故障排查)

---

## 技术债登记表（待优化）

> 这是一个已知问题清单，不是 bug。修复时从这里挑，改完在 CHANGELOG 补记录并从本表移除。

| # | 优先级 | 问题 | 影响范围 | 建议方案 | 关联文件 |
|---|---|---|---|---|---|
| TD-2 | **中** | `calcAndSaveStat()` 当前写死为"生成模拟数据"占位实现 | 业务核心 | 对接真实订单表（或其他数据源）做 SUM/COUNT 聚合；或改为接受外部 Mapper 注入 | `ReportStatServiceImpl.java` `ReportStatMapper.xml` |
| TD-4 | **低** | `ReportStatMapper.selectByDateRange(LocalDate, LocalDate)` 接口暴露但 Service 层从未调用（Service 改用 LambdaQueryWrapper 绕开） | 低风险误用 | 删除该 Mapper 方法声明 + XML，或改为 Service 层统一调用 | `ReportStatMapper.java` `ReportStatMapper.xml` |
| TD-7 | **低** | `application.yml` 用 `mybatis:` 前缀，而依赖是 `mybatis-plus-boot-starter`。部分属性兼容，但 `type-aliases-package` 在 mybatis-plus 下应改用 `type-aliases-packages` 且前缀改为 `mybatis-plus:` | 配置一致性 | 统一改为 `mybatis-plus:` 前缀，并在 yml 中显式声明 `mybatis-plus.configuration.map-underscore-to-camel-case: true` | `application.yml` |
| TD-8 | **建议** | 无单元测试，只有一个 TestController 做手动连通性测试 | 覆盖率 | 补 Service 层 Mock 测试 + Controller 层 MockMvc 测试 | `src/test/` |

---

## Bug 修复历史（Changelog）

### v1.2 — 2026-11-07 报表查询参数强校验（TD-5）

| # | 文件 | 问题 | 修复 |
|---|---|---|---|
| TD-5 | `pom.xml` `ReportQuery.java` `ReportController.java` | 查询参数无必填/格式/范围校验，非法入参依赖底层转换异常 | 引入 `spring-boot-starter-validation`；`@NotNull` + `@DateTimeFormat` + `@AssertTrue` 校验 startDate ≤ endDate；`submit` 加 `@Valid`，非法参数返回 400 |
| TD-6 | `ReportExportServiceImpl.java` `ReportController.java` | 导出文件与下载名使用 Long 自增主键（如 `9.xlsx`），可被顺序猜测 | 文件名改用 `task.getTaskId()`（32 位 UUID），存储路径与下载响应头保持一致 |
| TD-1 | `ReportExportAsyncService.java` `ReportExportServiceImpl.java` `ReportExportService.java` | 用 `@Lazy` 自注入绕过 `@Async` 自调用，依赖隐式代理 | 异步导出逻辑抽到独立 `ReportExportAsyncService`（`@Async("reportExportPool")`），原实现构造注入并调用它，删除自注入 |
| TD-3 | `QuartzConfig.java` `TaskTimeoutCheckJob.java` | 三个 Job cron 为 30 秒调试频率；超时检测 Redis key 用自增主键且无 TTL；日志用 `System.out` | cron 改生产值（02:00 / 03:00 / 每小时）；key 改 `report:task:{taskId}` 并设 1 天 TTL；日志改 `@Slf4j` |
| 缺陷 | `JsonUtil.java` | 裸 `ObjectMapper` 未注册 `JavaTimeModule`，序列化含 `LocalDateTime` 的 `ReportTask` 失败并返回 `null`，导致超时任务 Redis 缓存从未写入 | 注册 `JavaTimeModule` 并关闭 `WRITE_DATES_AS_TIMESTAMPS`；`toJson` 恢复正常输出 |

### v1.1 — 2026-10-18 修复 18 个 bug

#### P0 阻断级（启动/核心功能失效）

| # | 文件 | 问题 | 修复 |
|---|---|---|---|
| P0-1 | `application.yml` | 所有配置键使用 **Unicode 全角减号 U+2011**（如 `job‑store‑type`），Spring Boot relaxed binding 无法识别，**全部属性注入失效** | 全文替换为 ASCII `-` |
| P0-2 | `ThreadPoolConfig.java` | `@Value("${task.pool.core‑size}")` 同含全角减号，线程池配置无法注入 | 三处全改 ASCII `-` |
| P0-3 | `TaskStatusEnum.java` | 枚举值 `RUNNING`/`SUCCESS` 与 DB 实际存储的 `RUN`/`SUC` **不一致**，超时检测永远漏匹配 | 改为 `RUN("RUN",1)` `SUC("SUC",2)` |
| P0-4 | `ReportTaskMapper.xml` | `selectRunningTimeoutTasks` 用 `task_status=1` 对 **varchar 列做数字比较**，MySQL 隐式转换 `'RUN'→0`，**永远查不到** | 改为 `task_status='RUN'` |
| P0-5 | `ReportExportServiceImpl.java` | `asyncExportTask` 设置状态为 `"PROCESSING"`（枚举中不存在），**与 DB 完全脱节** | 改为 `TaskStatusEnum.RUN.status` 即 `"RUN"` |
| P0-6 | `TestController.java` | 日期字符串 `"2026‑10‑04"` 含全角减号，**永远查不到记录** | 改为 ASCII `"2026-10-04"` |

#### P1 功能缺陷级（运行时异常/功能缺失）

| # | 文件 | 问题 | 修复 |
|---|---|---|---|
| P1-7 | `ReportExportServiceImpl.java` | `@Async` **自调用不生效**：`createTask` 直接调同类 `asyncExportTask`，Spring AOP 不拦截自调用，**导出变同步阻塞** | 注入 `@Lazy ReportExportService self`，通过代理调 `self.asyncExportTask(...)` |
| P1-8 | `ReportStat.java` | **缺少 `@TableId(type=IdType.AUTO)`**，MyBatis-Plus 默认雪花 ID 与表 AUTO_INCREMENT 冲突 | 补充注解 |
| P1-9 | `ReportStat.java` | **缺少 `@TableName("report_stat")`**，BaseMapper 无法正确映射表名 | 补充注解 |
| P1-10 | `ReportStatServiceImpl.java` | `listRangeDate` 用 `LambdaQueryWrapper.between(ReportStat::getStatDate, LocalDate, LocalDate)`，但 `statDate` 是 **String 字段**，LocalDate 与 String 比较结果未定义 | 先 `LocalDate.format("yyyy-MM-dd")` 转为 String 再比较 |
| P1-11 | `ReportExportServiceImpl.java` | `createTask` **未设置 expireTime**，所有任务 `expire_time=NULL`，`selectExpireTasks WHERE expire_time < NOW()` **永远返回空**，FileCleanJob 永远不清理 | 根据 `report.file.expire-day` 设置 `expireTime = LocalDateTime.now().plusDays(expireDay)` |
| P1-12 | `ReportExportServiceImpl.java` | `createTask` **未设置 reportType**，所有任务 `report_type=NULL` | 默认 `"STAT"` |
| P1-13 | `ReportStatServiceImpl.java` | `calcAndSaveStat()` **方法体为空**，ReportStatJob 每 30 秒调但无任何效果 | 实现：先查今日 stat_date 是否已存在，去重后插入模拟统计数据 |

#### P2 代码质量级（不影响运行但存在隐患）

| # | 文件 | 问题 | 修复 |
|---|---|---|---|
| P2-14 | `ReportExportServiceImpl.java` | `@Async` 未指定线程池，`ThreadPoolConfig` 定义了 `reportExportPool` 但**从未被引用** | 改为 `@Async("reportExportPool")` |
| P2-15 | `ReportTaskMapper.java` | XML 中 `insertTask`/`updateTask` 有定义但**接口无对应方法声明**，MyBatis 启动时可能警告 | 补充两个接口方法声明 |
| P2-16 | `ReportStat.java` | EasyExcel 导出用 `ReportStat.class` 但**无 `@ExcelProperty`**，列头为英文字段名 | 添加中文 `@ExcelProperty` 注解 |
| P2-17 | `ReportStat.java` | `createTime` 是 `LocalDateTime` 类型，EasyExcel 默认不支持，**运行时可能抛异常** | 添加 `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")` |
| P2-18 | `ReportTask.java` | 手写 getter/setter 与其他实体风格不一致（其他用 `@Data`） | 改用 `@Data` 注解 |
| P2-19 | `TaskTimeoutCheckJob.java` | 用 `TaskStatusEnum.FAIL.name()` 虽然值对但语义不清晰 | 改为 `TaskStatusEnum.FAIL.status` |

---

## 技术栈 & 依赖

| | 版本 | 说明 |
|---|---|---|
| **JDK** | 8 | `maven.compiler.source/target=8` |
| **Spring Boot** | 2.7.18 | parent 继承 |
| **Bean Validation** | spring-boot-starter-validation（Boot 管理） | 参数校验（Hibernate Validator） |
| **Maven** | 3.x | 构建工具 |
| **MySQL** | 5.7+ / 8.x | 数据库，驱动 `mysql-connector-j`（runtime scope） |
| **MyBatis-Plus** | 3.5.3.1 | ORM 框架，含 BaseMapper、IService、LambdaQueryWrapper |
| **EasyExcel** | 3.3.2 | Excel 读写（导出报表） |
| **Quartz** | spring-boot-starter-quartz | 定时任务调度 |
| **Spring Data Redis** | spring-boot-starter-data-redis | Redis 客户端（超时检测写 Redis） |
| **Redisson** | 3.23.5 | 分布式锁（导出幂等） |
| **Lombok** | （parent） | 简化实体类 |

**阿里云 Maven 仓库**：`https://maven.aliyun.com/repository/public`

---

## 目录结构

```
report_service/
├── .trae/rules/project_rules.md               # ⭐ 开发协作规则（必须遵守）
├── FIX_LOG.md                                 # 修改记录（每次修改追加）
├── pom.xml                                    # Maven 配置
├── src/main/
│   ├── java/com/report/
│   │   ├── ReportServiceApplication.java      # ⭐ 启动类
│   │   ├── config/
│   │   │   ├── QuartzConfig.java              #   Quartz Job/Trigger Bean 注册
│   │   │   └── ThreadPoolConfig.java           #   ThreadPoolExecutor Bean
│   │   ├── controller/
│   │   │   ├── ReportController.java           #   报表业务 API
│   │   │   └── TestController.java             #   Mapper 连通性测试
│   │   ├── entity/
│   │   │   ├── ReportData.java                 #   EasyExcel 导出模型（未使用）
│   │   │   ├── ReportQuery.java                #   查询参数 DTO（startDate/endDate）
│   │   │   ├── ReportStat.java                 #   实体 ↔ report_stat 表
│   │   │   ├── ReportTask.java                 #   实体 ↔ report_task 表
│   │   │   └── TaskStatusEnum.java             #   任务状态枚举
│   │   ├── job/
│   │   │   ├── FileCleanJob.java               #   清理过期导出文件
│   │   │   ├── ReportStatJob.java              #   计算并保存当日统计
│   │   │   └── TaskTimeoutCheckJob.java        #   标记超时运行任务
│   │   ├── mapper/
│   │   │   ├── ReportStatMapper.java           #   report_stat Mapper 接口
│   │   │   └── ReportTaskMapper.java           #   report_task Mapper 接口
│   │   ├── service/
│   │   │   ├── ReportExportService.java        #   导出服务接口
│   │   │   ├── ReportStatService.java          #   统计服务接口
│   │   │   └── impl/
│   │   │       ├── ReportExportServiceImpl.java  #   导出任务编排（建任务/查询/同步导出）
│   │   │       ├── ReportExportAsyncService.java # ⭐ 异步导出执行（@Async + 分布式锁 + 状态流转）
│   │   │       └── ReportStatServiceImpl.java     #   统计服务实现
│   │   └── util/
│   │       └── JsonUtil.java                   #   Jackson 序列化工具
│   └── resources/
│       ├── application.yml                     # ⭐ 应用配置
│       └── mapper/
│           ├── ReportStatMapper.xml            #   report_stat SQL
│           └── ReportTaskMapper.xml            #   report_task SQL
└── export/                                     # 运行时生成的导出文件目录（.gitignore 应忽略）
```

---

## 配置参考

`application.yml` 逐项说明：

```yaml
server:
  port: 8080                     # HTTP 端口

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/report_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root               # MySQL 用户名
    password: 123456             # MySQL 密码
  redis:
    host: 127.0.0.1              # Redis 地址
    port: 6379
    database: 0
  quartz:
    job-store-type: memory       # memory | jdbc（当前内存模式，重启丢失调度状态）
    properties:
      org.quartz.threadPool.threadCount: 5

report:
  file:
    temp-path: ./report_temp/    # 临时文件目录（当前未实际使用）
    expire-day: 7                # 导出文件过期天数，超过即被 FileCleanJob 删除

task:
  pool:
    core-size: 4                 # reportExportPool 核心线程数
    max-size: 8                  # 最大线程数
    queue-capacity: 20           # 队列容量

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.report.entity
```

**⚠️ 注意**：所有键名使用 **ASCII 连字符 `-`**。Unicode 全角减号 U+2011 会导致 Spring Boot 配置绑定完全失效（v1.1 修复了这个问题）。

**环境迁移最小配置**：只需改 `spring.datasource.*` 和 `spring.redis.*`。

---

## 数据库表结构

### report_stat（统计数据）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint(20) | PK, AUTO_INCREMENT | 主键 |
| stat_date | varchar(20) | | 统计日期，格式 `yyyy-MM-dd`（字符串） |
| total_order | int(11) | | 订单总数 |
| total_amount | decimal(18,2) | | 订单总金额 |
| create_time | datetime | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### report_task（导出任务）

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | bigint(20) | PK, AUTO_INCREMENT | 主键（DB 自增） |
| task_id | varchar(64) | **UK, NOT NULL** | 业务唯一ID（Java UUID 生成） |
| report_type | varchar(64) | | 报表类型，默认 `"STAT"` |
| task_status | varchar(16) | **NOT NULL** | 状态值，见 TaskStatusEnum |
| file_path | varchar(256) | | 本地文件路径 |
| download_url | varchar(256) | | 预留下载 URL 字段 |
| fail_msg | text | | 失败原因 |
| total_count | bigint(20) | DEFAULT 0 | 总条数 |
| finish_count | bigint(20) | DEFAULT 0 | 已处理条数 |
| create_time | datetime | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| expire_time | datetime | | 文件过期时间（create + expire-day） |

**约束**：`report_task.task_id` 有唯一索引 `uk_task_id`

---

## 实体 ↔ 表字段映射

### ReportStat

| Java 字段 | 类型 | DB 列 | MyBatis-Plus 注解 |
|---|---|---|---|
| id | Long | id | `@TableId(type = IdType.AUTO)` |
| statDate | String | stat_date | — |
| totalOrder | Integer | total_order | — |
| totalAmount | BigDecimal | total_amount | — |
| createTime | LocalDateTime | create_time | — |

### ReportTask

| Java 字段 | 类型 | DB 列 | MyBatis-Plus 注解 |
|---|---|---|---|
| id | Long | id | `@TableId(type = IdType.AUTO)` |
| taskId | String | task_id | —（唯一业务键，非自增） |
| reportType | String | report_type | — |
| taskStatus | String | task_status | — |
| filePath | String | file_path | — |
| downloadUrl | String | download_url | — |
| failMsg | String | fail_msg | — |
| totalCount | Long | total_count | — |
| finishCount | Long | finish_count | — |
| createTime | LocalDateTime | create_time | — |
| updateTime | LocalDateTime | update_time | — |
| expireTime | LocalDateTime | expire_time | — |

> **注意**：所有 DB 列名用下划线命名，Java 字段用驼峰命名。MyBatis 开启默认 `mapUnderscoreToCamelCase=true`（Spring Boot 默认），无需显式写 `<resultMap>`。

---

## 核心业务规则

| # | 规则 | 实现位置 |
|---|---|---|
| 1 | **任务状态机**：WAIT → RUN → SUC / FAIL，由 `TaskStatusEnum` 定义，DB 存 String 值 | `TaskStatusEnum` `ReportExportServiceImpl` `TaskTimeoutCheckJob` |
| 2 | **异步导出**：提交任务立即返回 taskId，后台 `reportExportPool` 线程池异步执行 | `ReportExportServiceImpl.createTask` + `ReportExportAsyncService.asyncExportTask` |
| 3 | **分布式锁**：每个 taskId 加 Redisson 锁（3s 等待 / 60s 持有），防止重复导出 | `ReportExportAsyncService.asyncExportTask` |
| 4 | **超时检测**：RUN 状态 且 `update_time` 距现在 > 30 分钟 → 标记 FAIL + 写 Redis（key `report:task:{taskId}`，TTL 1 天） | `TaskTimeoutCheckJob`（cron `0 0 * * * ?`） |
| 5 | **文件过期**：`expire_time = now + expire-day(7)` 后 → FileCleanJob 删除物理文件并置空 `file_path` | `ReportExportServiceImpl.createTask` 设置 expireTime；`FileCleanJob` 清理并清空路径 |
| 6 | **导出文件路径**：`{user.dir}/export/{taskId}.xlsx`（`taskId` 为 32 位 UUID，与 `task_id` 一致；下载文件名同） | `ReportExportAsyncService.asyncExportTask` |
| 7 | **统计去重**：`calcAndSaveStat` 执行前查 `stat_date` 是否已存在，防止重复插入 | `ReportStatServiceImpl.calcAndSaveStat` |
| 8 | **日期范围查询**：`stat_date` 是 varchar，查询前 `LocalDate.format("yyyy-MM-dd")` 转 String | `ReportStatServiceImpl.listRangeDate` |
| 9 | **下载前置校验**：只有 SUC 状态 + 文件存在才能下载，否则 400/404 | `ReportController.download` |
| 10 | **EasyExcel LocalDateTime**：必须加 `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")` 否则运行时异常 | `ReportStat.createTime` 字段 |

---

## 后端接口清单

### ReportController（类级 `@RequestMapping("/report")`）

| # | Method | URL | 方法 | 返回 | 说明 |
|---|---|---|---|---|---|
| 1 | **POST** | `/report/export/submit` | `submit(@RequestBody ReportQuery)` | `String` (taskId) | 提交异步导出任务 |
| 2 | **GET** | `/report/export/query?taskId=xxx` | `query(String taskId)` | `ReportTask` | 查询任务详情；taskId 不存在返回 404 |
| 3 | **GET** | `/report/download?taskId=xxx` | `download(String taskId, HttpServletResponse)` | Excel 流 | 下载导出文件；任务不存在/文件缺失 404，非 SUC 400 |

### TestController

| # | Method | URL | 方法 | 返回 | 说明 |
|---|---|---|---|---|---|
| 4 | **GET** | `/test/mapper` | `testMapper()` | `String` | Mapper 连通性测试 |

### 请求/响应示例

**POST /report/export/submit**
```json
// 请求体
{
  "startDate": "2026-02-01",
  "endDate": "2026-05-31"
}

// 响应
"9f6065289e694fb0aaf5d32b352d7cc2"
```

**GET /report/export/query?taskId=9f6065289e694fb0aaf5d32b352d7cc2**
```json
// 响应
{
  "id": 1,
  "taskId": "9f6065289e694fb0aaf5d32b352d7cc2",
  "reportType": "STAT",
  "taskStatus": "SUC",
  "filePath": "C:\\...\\export\\1.xlsx",
  "downloadUrl": null,
  "failMsg": null,
  "totalCount": 4,
  "finishCount": 4,
  "createTime": "2026-10-04T17:02:27",
  "updateTime": "2026-10-04T17:02:26",
  "expireTime": "2026-10-11T17:02:27"
}
```

---

## 类速查表

### 启动类

| 类 | 包 | 核心注解 |
|---|---|---|
| `ReportServiceApplication` | `com.report` | `@SpringBootApplication @MapperScan("com.report.mapper") @EnableAsync @EnableScheduling` |

### 枚举

| 枚举值 | status | code |
|---|---|---|
| WAIT | "WAIT" | 0 |
| RUN | "RUN" | 1 |
| SUC | "SUC" | 2 |
| FAIL | "FAIL" | 3 |

### Service 接口

| 接口 | 实现类 | 继承 | 核心方法 |
|---|---|---|---|
| `ReportStatService` | `ReportStatServiceImpl` | `IService<ReportStat>` | `listRangeDate(LocalDate, LocalDate)` `calcAndSaveStat()` |
| `ReportExportService` | `ReportExportServiceImpl` | 无 | `createTask(ReportQuery)` `syncExport(...)` `getTaskInfo(String)` |
| 无（独立异步 Bean） | `ReportExportAsyncService` | `ServiceImpl<ReportTaskMapper, ReportTask>` | `asyncExportTask(Long, LocalDate, LocalDate)`（`@Async("reportExportPool")`） |

### Mapper 接口（均 extends `BaseMapper<T>`）

| 接口 | 方法 | SQL 位置 |
|---|---|---|
| `ReportStatMapper` | `insertStat` / `selectByDate` / `selectByDateRange` | `ReportStatMapper.xml` |
| `ReportTaskMapper` | `insertTask` / `updateTask` / `selectByTaskId` / `selectRunningTimeoutTasks` / `selectExpireTasks` | `ReportTaskMapper.xml` |

### Job（均实现 `org.quartz.Job`）

| Job 类 | Cron | 功能 |
|---|---|---|
| `ReportStatJob` | `0 0 2 * * ?`（每天 02:00） | 统计数据计算（调 `calcAndSaveStat`） |
| `FileCleanJob` | `0 0 3 * * ?`（每天 03:00） | 清理过期文件（查 `selectExpireTasks` + `file.delete`），成功后置空 `file_path`；路径为空或文件不存在时跳过并清空路径 |
| `TaskTimeoutCheckJob` | `0 0 * * * ?`（每小时整点） | 超时检测（查 `selectRunningTimeoutTasks(30)` → 标记 FAIL + 写 Redis，TTL 1 天） |

---

## 常用命令

### 前置条件

| 依赖 | 启动方式 |
|---|---|
| MySQL 5.7+ | Navicat 或 CLI 执行 DDL，建库 `report_db`，导入表结构 |
| Redis | `redis-server` 或 Windows `redis-server.exe` |

### Maven 命令

```bash
# 编译（首次运行需联网下载依赖）
mvn compile

# 跳过测试打包
mvn clean package -DskipTests

# 本地启动（开发模式）
mvn spring-boot:run

# 打包后运行
java -jar target/report_service-1.0-SNAPSHOT.jar

# 指定 profile 运行（如果你加了 application-dev.yml）
java -jar target/report_service-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 启动后验证

```bash
# PowerShell — 提交导出任务
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/report/export/submit" -ContentType "application/json" -Body '{"startDate":"2026-02-01","endDate":"2026-05-31"}'

# 查询任务状态（把 <taskId> 替换为上一步返回值）
Invoke-RestMethod -Uri "http://localhost:8080/report/export/query?taskId=<taskId>"

# 下载 Excel（浏览器或 curl）
curl -o report.xlsx "http://localhost:8080/report/download?taskId=<taskId>"

# Mapper 连通性测试
Invoke-WebRequest -Uri "http://localhost:8080/test/mapper" -UseBasicParsing
```

---

## 扩展指南

### 新增报表类型（从 STAT 扩展到更多业务报表）

1. **新增枚举值**：在 `ReportTask.report_type` 字段约定新值（如 `"ORDER_SUMMARY"`）
2. **Mapper 层**：在对应业务 Mapper 加自定义查询 SQL
3. **Service 层**：在 `ReportExportAsyncService` 的 `asyncExportTask` 中根据 `reportType` 分支调用不同数据源
4. **实体**：如果导出列不同，新建一个带 `@ExcelProperty` 的导出模型类（参考 `ReportData.java`），EasyExcel 的第二个参数传新类的 `.class`

### 新增定时任务

1. **Job 类**：在 `com.report.job` 下新建类，实现 `org.quartz.Job`，加 `@Component`
2. **QuartzConfig**：新增两个 Bean —— `JobDetail` 和 `Trigger`，用 `CronScheduleBuilder.cronSchedule("你的cron")`
3. **Cron 工具**：https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html

### 新增导出格式（PDF / CSV 等）

1. 在 `ReportExportService` 接口加新方法签名（如 `exportPdf`）
2. `ReportExportServiceImpl` 实现，可复用分布式锁 + 状态机逻辑
3. `createTask` 中根据参数选择导出格式

### 新增数据库表

1. MySQL 建表（保持下划线命名）
2. `entity/` 下新建实体类，**必须加** `@TableName` + `@TableId(type = IdType.AUTO)`（对应表 AUTO_INCREMENT）
3. `mapper/` 下新建 Mapper 接口，**继承** `BaseMapper<你的实体>`，加 `@Mapper`
4. 如需自定义 SQL，在 `resources/mapper/` 下新建 XML，namespace 为接口全限定名
5. 如果 Service 层需要，**继承** `ServiceImpl<Mapper, 实体>` + 实现 `IService<实体>`

### 新增 Controller

1. 在 `controller/` 下新建类，加 `@RestController`
2. 类级 `@RequestMapping` 加统一前缀（如 `/admin`、`/api/v2`）
3. 方法级加 `@GetMapping` / `@PostMapping` 等
4. 参数校验用 `@Valid` + `@NotNull` 等（项目已引入 validation starter）

---

## 开发约定

| 项 | 约定 | 依据 |
|---|---|---|
| **包结构** | 按 controller → service → mapper → entity 分层 | 目录结构 |
| **Mapper 扫描** | `@MapperScan("com.report.mapper")`，启动类统一扫描 | `ReportServiceApplication.java` |
| **Mapper XML 位置** | `resources/mapper/*.xml`，namespace 为接口全限定名 | `application.yml` 的 `mapper-locations` |
| **主键策略** | 所有表 `AUTO_INCREMENT`，实体必须 `@TableId(type = IdType.AUTO)` | ReportStat / ReportTask 两个实体 |
| **列命名** | DB 用下划线，Java 用驼峰，**MyBatis-Plus 默认开启** `mapUnderscoreToCamelCase=true`（可在 `application.yml` 显式声明） | mybatis-plus-boot-starter 默认行为 |
| **Service 继承** | ServiceImpl 继承 `ServiceImpl<Mapper, 实体>` + 实现 Service 接口 | ReportExportServiceImpl / ReportStatServiceImpl |
| **异步线程池** | Bean 名 `reportExportPool`，`@Async("reportExportPool")` 引用；线程名 `report-export-pool-N`，便于 jstack 排查 | `ThreadPoolConfig.java` |
| **分布式锁** | Redisson，key 前缀 `report:export:lock:`，3s 等待 / 60s 持有 | `ReportExportAsyncService.EXPORT_LOCK` |
| **状态值** | DB 存 String，枚举的 `status` 字段即 DB 值（不是 `.name()`） | `TaskStatusEnum`（v1.1 修复了 `.name()` vs `.status` 的混用） |
| **Excel 日期格式** | LocalDateTime 字段必须加 `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")` | `ReportStat.createTime` 字段 |
| **配置键名** | 全部使用 **ASCII hyphen `-`**，禁用 Unicode U+2011 全角减号 | v1.1 修复了这个致命问题 |
| **导出文件命名** | `{user.dir}/export/{UUID}.xlsx` | `ReportExportAsyncService.asyncExportTask` |

---

## 开发协作规范

> 完整规则见 [`.trae/rules/project_rules.md`](.trae/rules/project_rules.md)，修改记录见 [`FIX_LOG.md`](FIX_LOG.md)。

所有涉及代码/配置/文档的修改，均按「分析 → 确认 → 执行 → 收尾」闭环执行：

| 阶段 | 要求 |
|---|---|
| **1. 问题分析** | 需求提出后不直接改代码，先输出：需求理解、涉及文件/模块、风险与影响范围、分步方案、编号化待确认决策点 |
| **2. 确认后执行** | 用户回复（如「1同意 2选B」）后才修改；严格按确认方案，不扩大范围；方案不可行则停止并回到分析 |
| **3. 自动收尾** | 追加 `FIX_LOG.md`；同步 README 相关章节；执行编译/测试/启动自检并输出「自检报告」 |
| **4. 放弃/回滚** | 回复「放弃」即终止；回滚优先用 git，执行破坏性命令前必须获得明确许可 |

约定要点：一次只处理一个已确认任务；不改无关文件；不删除现有功能；关键文件修改前说明回滚方式；全部使用中文、决策点编号化。

---

## 故障排查

| 现象 | 可能原因 | 排查步骤 |
|---|---|---|
| 启动报错 `BeanCreationException: Error creating bean with name 'taskThreadPoolConfig'` | `@Value` 键名与 yml 不匹配 | 检查 `application.yml` 键名和 `@Value` 字符串是否完全一致（尤其注意 `-` 是 ASCII） |
| `mvn compile` 报 `Could not resolve dependencies` | 本地 Maven 仓库缓存损坏 | `mvn dependency:purge-local-repository` 或手动删 `~/.m2/repository` 下对应目录 |
| 启动报 `AccessDeniedException`（Maven 沙箱环境） | Agent 沙箱限制了 `~/.m2` 写入 | 在 IDEA 本地终端直接跑 `mvn spring-boot:run` |
| `/test/mapper` 返回 "超时任务数量:0" 且超时分钟参数 > 30 | 正常——30 分钟内完成的任务不算超时 | 把 timeoutMinute 改小或手动造一个 RUN 状态且 update_time 很旧的 task |
| EasyExcel 导出报 `UnsupportedOperationException` on LocalDateTime | 缺 `@DateTimeFormat` 注解 | 在实体 LocalDateTime 字段加 `@DateTimeFormat("yyyy-MM-dd HH:mm:ss")` |
| Redis 连接失败报 `Unable to connect to Redis` | Redis 没启动 | `redis-server` 启动，或改 `spring.redis.host/port` |
| Redisson 分布式锁报 `RedisException` | Redis 不可达 | 同上 |
| 下载返回 404 但 taskStatus=SUC | 文件被 FileCleanJob 删掉了，或 `file_path` 已被置空 | 查 `expire_time` 是否已过、`file_path` 是否为空，或手动到 `export/` 目录确认文件是否存在 |
| 任务状态一直是 RUN 不变成 SUC | 锁拿不到导致提前 return | 查 Redis 是否正常，或多个实例并发导出同一个 taskId |
| `/report/export/submit` 返回 taskId 但文件没生成 | @Async 没生效 | 确认 `ReportExportServiceImpl` 注入的是 `ReportExportAsyncService`，调的是它的 `asyncExportTask()`；同时确认 `@EnableAsync` 与 `reportExportPool` 存在 |
| Quartz Job 执行频率不符合预期 | 生产 cron 与调试期不同 | 查看 `QuartzConfig.java` 三个 Trigger 的 cron（统计 02:00、清理 03:00、超时检测每小时）；临时调试可改回 `*/30 * * * * ?` |

---

## 附录：原始 DDL（Navicat 导出）

> 来自用户提供的原始建表脚本，仅供参考。**禁止擅自修改实体字段或 DB 列名**——如需加列/改列，必须同步改实体、Mapper XML、application.yml（如加配置）。

### report_stat

```sql
CREATE TABLE `report_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` varchar(20) DEFAULT NULL,
  `total_order` int(11) DEFAULT NULL,
  `total_amount` decimal(18,2) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=5;
```

### report_task

```sql
CREATE TABLE `report_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` varchar(64) NOT NULL COMMENT '唯一任务ID',
  `report_type` varchar(64) DEFAULT NULL COMMENT '报表类型',
  `task_status` varchar(16) NOT NULL COMMENT '任务状态 WAIT等待 RUN执行 SUC成功 FAIL失败',
  `file_path` varchar(256) DEFAULT NULL COMMENT '文件本地路径',
  `download_url` varchar(256) DEFAULT NULL COMMENT '下载地址',
  `fail_msg` text COMMENT '失败信息',
  `total_count` bigint(20) DEFAULT 0 COMMENT '总条数',
  `finish_count` bigint(20) DEFAULT 0 COMMENT '已处理条数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime DEFAULT NULL COMMENT '文件过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=8;
```
