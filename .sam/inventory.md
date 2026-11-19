# Confirmed Inventory — Cloud Spanner Migration

## Confirmed Stack & Target Overview

- **Language:** Java 17 ([`pom.xml#L17`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/pom.xml#L17))
- **Build Tool:** Apache Maven 3.9.12 / Wrapper 3.3.4 ([`maven-wrapper.properties#L1-L3`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/.mvn/wrapper/maven-wrapper.properties#L1-L3))
- **Access / ORM Layer:** MyBatis 3.5.19 ([`flowable-dependencies/pom.xml#L47`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dependencies/pom.xml#L47))
- **Target Database:** Google Cloud Spanner
- **Target Dialect:** GoogleSQL
- **Target Driver:** Cloud Spanner JDBC Driver (`com.google.cloud:google-cloud-spanner-jdbc`)
- **Status:** Developer confirmed with override to proceed.

---

## Architectural Context

Flowable is a multi-engine workflow and decision platform consisting of multiple sub-engines:
1. **Engine Common (`flowable-engine-common`):** Provides core database session management (`DbSqlSession`, `DbSqlSessionFactory`), SQL script schema execution (`AbstractSqlScriptBasedDbSchemaManager`), database dialect detection (`DbUtil`), command interceptors, and common caching/properties.
2. **Process Engine (`flowable-engine`):** BPMN engine core, execution tree, user tasks, job scheduling, process definition repository, and history.
3. **CMMN Engine (`flowable-cmmn-engine`):** Case management engine and associated history.
4. **DMN Engine (`flowable-dmn-engine`):** Decision table execution engine and history.
5. **IDM Engine (`flowable-idm-engine`):** Identity management (users, groups, privileges, tokens).
6. **App Engine (`flowable-app-engine`):** Application deployment definitions.
7. **Event Registry (`flowable-event-registry`):** Event channel and definition management.
8. **Service Modules:** Specialized services (`flowable-task-service`, `flowable-variable-service`, `flowable-job-service`, `flowable-batch-service`, `flowable-identitylink-service`, `flowable-entitylink-service`, `flowable-eventsubscription-service`).

Data persistence across all engines relies on MyBatis with parameterized SQL statements in XML mapping files, using optimistic concurrency control (`REV_` column) and table/session management.

---

## Candidate Files & Migration Scope by Work Unit Group

### Group 1: Build & Dependency Management

| File | Change Summary | References / Rationale |
| :--- | :--- | :--- |
| [`modules/flowable-dependencies/pom.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dependencies/pom.xml#L62-L72) | Add `<google-cloud-spanner-jdbc.version>` property (e.g. `2.27.0`) and managed dependency for `com.google.cloud:google-cloud-spanner-jdbc` under `<!-- JDBC Driver dependencies -->`. | [`JDBC-C1`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L14): Pulls official JDBC driver. |
| [`modules/flowable-parent/pom.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-parent/pom.xml#L165-L220) | Add `<profile><id>spanner</id>` with `com.google.cloud:google-cloud-spanner-jdbc` dependency alongside existing `mysql`, `postgresql`, `mssql`, `oracle`, `db2` profiles. | Preserves existing database profiles while enabling Spanner driver on demand. |

---

### Group 2: Database Dialect Resolution & Engine Configuration

| File | Change Summary | References / Rationale |
| :--- | :--- | :--- |
| [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/util/DbUtil.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/util/DbUtil.java#L36-L78) | 1. Add `public static final String DATABASE_TYPE_SPANNER = "spanner";`<br>2. In `getDefaultDatabaseTypeMappings()`, map product name `"Google Cloud Spanner"` to `DATABASE_TYPE_SPANNER`.<br>3. In `determineDatabaseType()`, handle `jdbc:cloudspanner:` URL prefix if metadata lookup needs fallback. | [`JDBC-C8`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L132): `DatabaseMetaData.getDatabaseProductName()` returns `"Google Cloud Spanner"`. |
| [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L374-L385) | 1. Add `public static final String DATABASE_TYPE_SPANNER = "spanner";`<br>2. In `getDefaultCommandInterceptors()` ([`#L565-L575`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L565-L575)), add `SpannerRetryInterceptor` when `databaseType` is `spanner`.<br>3. In `initDatabaseType()` ([`#L476-L484`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L476-L484)), configure Spanner statement batch limit. | [`SPN-C1`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/conversion.md#L12), [`JDBC-C4`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L63): Handles Spanner transaction abort retries. |
| `modules/flowable-engine-common/src/main/resources/org/flowable/common/db/properties/spanner.properties` *(New File)* | Create Spanner database properties file with:<br>`limitAfter=LIMIT #{maxResults} OFFSET #{firstResult}`<br>`blobType=BYTES`<br>`boolValue=TRUE` | [`GSQL-C4`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L55): Spanner GoogleSQL pagination and byte typing. Loaded by [`AbstractEngineConfiguration#pathToEngineDbProperties()`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L854). |
| [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/ListQueryParameterObject.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/ListQueryParameterObject.java#L213-L248) | Add `AbstractEngineConfiguration.DATABASE_TYPE_SPANNER.equals(databaseType)` to the `NULLS FIRST` and `NULLS LAST` order-by branches. | GoogleSQL natively supports `ORDER BY col [ASC|DESC] NULLS FIRST/LAST`. |
| [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/persistence/entity/TableDataManagerImpl.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/persistence/entity/TableDataManagerImpl.java#L87-L99) | In `getTableNameFilter()` and `getTableMetaData()`, handle Spanner table casing (standard uppercase match) and default empty schema. | [`GSQL-C13`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L165): Default Spanner schema is `""`. |
| [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java#L137-L147) | In `isTablePresent()`, ensure schema is set to `null` or `""` when `databaseType` is `spanner`. | Avoids searching in nonexistent schema namespaces. |

---

### Group 3: Schema DDL Scripts (Create & Drop)

Flowable's schema managers execute SQL scripts resolved via:
`org/flowable/<component>/db/<create|drop>/flowable.<databaseType>.<create|drop>.<component>.sql`.

The following **15 schema DDL files** must be authored for Cloud Spanner (GoogleSQL):

| Submodule | Target File Path | Description & Schema Elements |
| :--- | :--- | :--- |
| **`flowable-engine-common`** | `src/main/resources/org/flowable/common/db/create/flowable.spanner.create.common.sql` | `CREATE TABLE ACT_GE_PROPERTY (NAME_ STRING(64), VALUE_ STRING(300), REV_ INT64) PRIMARY KEY (NAME_);`<br>Initial inserts for `common.schema.version` and `next.dbid`. |
| **`flowable-engine-common`** | `src/main/resources/org/flowable/common/db/drop/flowable.spanner.drop.common.sql` | `DROP TABLE IF EXISTS ACT_GE_PROPERTY;` |
| **`flowable-engine`** | `src/main/resources/org/flowable/db/create/flowable.spanner.create.engine.sql` | Core BPMN tables: `ACT_RE_DEPLOYMENT`, `ACT_RE_MODEL`, `ACT_RE_PROCDEF`, `ACT_RU_EXECUTION`, `ACT_RU_EVENT_SUBSCR`, `ACT_RU_TASK`, `ACT_RU_VARIABLE`, `ACT_RU_IDENTITYLINK`, `ACT_RU_JOB`, `ACT_RU_TIMER_JOB`, `ACT_RU_SUSPENDED_JOB`, `ACT_RU_DEADLETTER_JOB`, `ACT_RU_HISTORY_JOB`, `ACT_RU_EXTERNAL_JOB`, `ACT_RU_ACTINST`, `ACT_RU_ENTITYLINK`, `ACT_GE_BYTEARRAY`, `ACT_PROCDEF_INFO`. Secondary indexes. |
| **`flowable-engine`** | `src/main/resources/org/flowable/db/create/flowable.spanner.create.history.sql` | BPMN History tables: `ACT_HI_PROCINST`, `ACT_HI_ACTINST`, `ACT_HI_TASKINST`, `ACT_HI_VARINST`, `ACT_HI_DETAIL`, `ACT_HI_COMMENT`, `ACT_HI_ATTACHMENT`, `ACT_HI_IDENTITYLINK`, `ACT_HI_ENTITYLINK`, `ACT_HI_TSK_LOG`. Secondary indexes. |
| **`flowable-engine`** | `src/main/resources/org/flowable/db/drop/flowable.spanner.drop.engine.sql` | Drop statements in reverse dependency order: `DROP TABLE IF EXISTS ACT_RU_*`, `ACT_RE_*`, `ACT_GE_BYTEARRAY`, `ACT_PROCDEF_INFO`. |
| **`flowable-engine`** | `src/main/resources/org/flowable/db/drop/flowable.spanner.drop.history.sql` | Drop statements for `ACT_HI_*` tables. |
| **`flowable-idm-engine`** | `src/main/resources/org/flowable/idm/db/create/flowable.spanner.create.identity.sql` | IDM tables: `ACT_ID_USER`, `ACT_ID_INFO`, `ACT_ID_GROUP`, `ACT_ID_MEMBERSHIP`, `ACT_ID_PRIV`, `ACT_ID_PRIV_MAPPING`, `ACT_ID_TOKEN`, `ACT_ID_BYTEARRAY`. |
| **`flowable-idm-engine`** | `src/main/resources/org/flowable/idm/db/drop/flowable.spanner.drop.identity.sql` | Drop statements for `ACT_ID_*` tables. |
| **`flowable-cmmn-engine`** | `src/main/resources/org/flowable/cmmn/db/create/flowable.spanner.create.cmmn.sql` | CMMN tables: `ACT_CMMN_DEPLOYMENT`, `ACT_CMMN_DEPLOYMENT_RESOURCE`, `ACT_CMMN_CASEDEF`, `ACT_CMMN_RU_CASE_INST`, `ACT_CMMN_RU_PLAN_ITEM_INST`, `ACT_CMMN_RU_SENTRY_PART_INST`, `ACT_CMMN_RU_MIL_INST`, `ACT_CMMN_HI_CASE_INST`, `ACT_CMMN_HI_MIL_INST`, `ACT_CMMN_HI_PLAN_ITEM_INST`. |
| **`flowable-cmmn-engine`** | `src/main/resources/org/flowable/cmmn/db/drop/flowable.spanner.drop.cmmn.sql` | Drop statements for `ACT_CMMN_*` tables. |
| **`flowable-dmn-engine`** | `src/main/resources/org/flowable/dmn/db/create/flowable.spanner.create.dmn.sql` | DMN tables: `ACT_DMN_DEPLOYMENT`, `ACT_DMN_DEPLOYMENT_RESOURCE`, `ACT_DMN_DECISION_TABLE`, `ACT_DMN_HI_DECISION_EXECUTION`. |
| **`flowable-dmn-engine`** | `src/main/resources/org/flowable/dmn/db/drop/flowable.spanner.drop.dmn.sql` | Drop statements for `ACT_DMN_*` tables. |
| **`flowable-app-engine`** | `src/main/resources/org/flowable/app/db/create/flowable.spanner.create.app.sql` | App tables: `ACT_APP_DEPLOYMENT`, `ACT_APP_DEPLOYMENT_RESOURCE`, `ACT_APP_APPDEF`. |
| **`flowable-app-engine`** | `src/main/resources/org/flowable/app/db/drop/flowable.spanner.drop.app.sql` | Drop statements for `ACT_APP_*` tables. |
| **`flowable-event-registry`** | `src/main/resources/org/flowable/eventregistry/db/create/flowable.spanner.create.eventregistry.sql` | Event registry tables: `FLW_EV_DEPLOYMENT`, `FLW_EV_DEPLOYMENT_RESOURCE`, `FLW_EVENT_DEFINITION`, `FLW_CHANNEL_DEFINITION`, `FLW_EVENT_RESOURCE`, `FLW_RU_BATCH`, `FLW_RU_BATCH_PART`. |
| **`flowable-event-registry`** | `src/main/resources/org/flowable/eventregistry/db/drop/flowable.spanner.drop.eventregistry.sql` | Drop statements for `FLW_*` tables. |
| **`distro`** | `distro/sql/create/all/flowable.spanner.all.create.sql` | Aggregated full create script for Spanner distribution. |

#### GoogleSQL DDL Syntax Transformations (from SAM Knowledge)
- Primary Key Syntax: `CREATE TABLE table_name ( ... ) PRIMARY KEY (ID_)`
- Type Conversions:
  - `VARCHAR(n)` → `STRING(n)`
  - `VARCHAR(4000)` / large text → `STRING(MAX)` ([`GSQL-C4`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L55))
  - `LONGBLOB` / `BLOB` / `bytea` → `BYTES(MAX)` ([`GSQL-C4`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L55))
  - `INTEGER` / `INT` / `TINYINT` / `BIGINT` → `INT64`
  - `BOOLEAN` / `BIT` → `BOOL`
  - `DATETIME(3)` / `TIMESTAMP(3)` → `TIMESTAMP`
- Secondary Indexes:
  - Standard: `CREATE INDEX idx_name ON table_name (col1, col2)`
  - Nullable unique indexes: `CREATE UNIQUE NULL_FILTERED INDEX idx_name ON table_name (col)` ([`GSQL-C5`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L68))
- Script Execution: Wrap DDL execution in `START BATCH DDL; ... RUN BATCH;` where supported ([`SPN-C5`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/conversion.md#L61)).

---

### Group 4: MyBatis XML Mapper Modifications

In several XML mappers, database-specific clauses check `_databaseId`:

| File | Line | Issue / Required Adjustment | Rationale |
| :--- | :--- | :--- | :--- |
| [`modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricDetail.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricDetail.xml#L219) | 219 | `delete <if test="_databaseId != 'postgres' and _databaseId != 'cockroachdb' and _databaseId != 'db2' and _databaseId != 'spanner'"> HIDETAIL </if> from ...` | GoogleSQL syntax does not support table alias between `DELETE` and `FROM`. Requires `DELETE FROM ...`. |
| [`modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricActivityInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricActivityInstance.xml#L288) | 288 | `delete <if test="_databaseId != 'postgres' and _databaseId != 'cockroachdb' and _databaseId != 'db2' and _databaseId != 'spanner'"> ACTINST </if> from ...` | GoogleSQL syntax requires `DELETE FROM ...`. |
| [`modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml#L291) | 291, 327 | `delete <if test="_databaseId != 'postgres' and _databaseId != 'cockroachdb' and _databaseId != 'db2' and _databaseId != 'spanner'"> VARINST </if> from ...` | GoogleSQL syntax requires `DELETE FROM ...`. |
| [`modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml#L223) | 223, 238 | `delete <if test="_databaseId != 'postgres' and _databaseId != 'cockroachdb' and _databaseId != 'db2' and _databaseId != 'spanner'"> TSKLOG </if> from ...` | GoogleSQL syntax requires `DELETE FROM ...`. |
| [`modules/flowable-engine/src/main/resources/org/flowable/db/mapping/ChangeTenantBpmn.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/ChangeTenantBpmn.xml#L100-L230) | 100–230 | Review `update ... FROM ...` statements for Spanner GoogleSQL compatibility. | GoogleSQL uses standard `UPDATE table SET ... WHERE ...` with subqueries. |

---

### Group 5: Transaction Management & Spanner Abort Retry Interceptor

| Component | Target Location | Change Summary | References / Rationale |
| :--- | :--- | :--- | :--- |
| **`SpannerRetryInterceptor`** | `modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/interceptor/SpannerRetryInterceptor.java` *(New Class)* | Author a retry interceptor modeled after `CrDbRetryInterceptor` ([`CrDbRetryInterceptor.java#L27-L95`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/interceptor/CrDbRetryInterceptor.java#L27-L95)), but customized for Spanner error classification: detect `AbortedException`, `AbortedDueToConcurrentModificationException`, and gRPC `ABORTED` in the exception cause chain. | [`JDBC-C2`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L36), [`JDBC-C4`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L63): Spanner `SQLState` is null; error detection must check exception type and gRPC message status prefix. |
| **Engine Interceptor Wiring** | [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L570-L573`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L570-L573) | In `getDefaultCommandInterceptors()`, instantiate and add `SpannerRetryInterceptor` when `DATABASE_TYPE_SPANNER.equals(databaseType)`. | Automatically retries aborted Flowable commands against Cloud Spanner. |

---

### Group 6: Primary Key & ID Generation Strategy

| File / Component | Current Implementation | Migration Recommendation | Rationale |
| :--- | :--- | :--- | :--- |
| [`DbIdGenerator.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/java/org/flowable/engine/impl/db/DbIdGenerator.java#L25-L48) | Uses `GetNextIdBlockCmd` on `ACT_GE_PROPERTY` (`next.dbid`) to generate monotonically increasing sequential numbers as strings (`"1"`, `"2"`, `"3"`). | In Spanner deployments, recommend configuring `StrongUuidGenerator` (`idGenerator = new StrongUuidGenerator()`) or UUID-based keys. | [`GSQL-C1`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L11), [`SPN-D7`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/decisions.md#L95): Monotonically increasing primary keys cause write hotspotting on the root split of distributed tables in Cloud Spanner. UUIDs distribute write throughput across splits. |

---

### Group 7: Test Harness & CI Verification

| File | Change Summary | Rationale |
| :--- | :--- | :--- |
| Test Configuration Templates (`modules/*/src/test/resources/flowable.*.cfg.xml`) | Already parameterized with `${jdbc.url}`, `${jdbc.driver}`, `${jdbc.username}`, `${jdbc.password}` via HikariCP ([`flowable.cfg.xml#L13-L16`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/resources/flowable.cfg.xml#L13-L16)). | Allows running existing test suites against Cloud Spanner Emulator or live instance by passing `-Djdbc.url=jdbc:cloudspanner://... -Djdbc.driver=com.google.cloud.spanner.jdbc.JdbcDriver -Pspanner`. |
| `.github/workflows/spanner.yml` *(New Workflow)* | Add CI workflow utilizing `gcr.io/cloud-spanner-emulator/emulator` on port 9010. | Enables continuous automated verification of the Spanner dialect and engine persistence. |

---

## Next Steps for Planner

With this confirmed inventory in place:
1. **Root Work Unit (Unit 1):** Maven dependencies (`flowable-dependencies/pom.xml`, `flowable-parent/pom.xml`) to include `google-cloud-spanner-jdbc` alongside existing drivers.
2. **Unit 2 (Dialect & Configuration):** `DbUtil.java`, `AbstractEngineConfiguration.java`, `spanner.properties`, `ListQueryParameterObject.java`, and `TableDataManagerImpl.java`.
3. **Unit 3 (Transaction & Retries):** `SpannerRetryInterceptor.java` and interceptor wiring.
4. **Unit 4 (Common & Engine Schema DDL):** `flowable.spanner.*.common.sql`, `flowable.spanner.*.engine.sql`, `flowable.spanner.*.history.sql`, and MyBatis mapper alias fixes.
5. **Unit 5 (Sub-engine Schema DDL):** IDM, CMMN, DMN, App, and Event Registry Spanner DDL scripts.
6. **Unit 6 (Verification & Test Harness):** Spanner emulator test suite and workflow.
