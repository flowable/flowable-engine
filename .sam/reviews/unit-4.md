---
unit: unit-4
round: 1
status: accept
dimensions:
  correctness: pass
  completeness: pass
  test-integrity: pass
evidence: |
  - ./mvnw test -pl modules/flowable-engine -am -Dtest=SpannerMyBatisMapperTest -Dsurefire.failIfNoSpecifiedTests=false: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
  - ./mvnw test -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service -am -Dtest=*Mapper*,*Spanner* -Dsurefire.failIfNoSpecifiedTests=false: Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
  - ./mvnw checkstyle:check -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service,modules/flowable-identitylink-service,modules/flowable-cmmn-engine: 0 Checkstyle violations
---

# Independent Review Assessment: Unit 4

## Overview
Unit 4 modifies MyBatis XML mapping definitions across the Flowable engine and service modules to guarantee compliance with Cloud Spanner's GoogleSQL dialect. Specifically, it eliminates MySQL/MSSQL-style table aliases placed directly between the `DELETE` and `FROM` keywords in dynamic SQL statements, and verifies that update statements in tenant manipulation mappers adhere to GoogleSQL requirements.

Review Status: **ACCEPT**
Rework Round: 1

---

## Dimension Evaluations

### 1. Correctness: PASS

- **GoogleSQL DML Syntax Compliance (`GSQL-C7`)**:
  - GoogleSQL specifies `DELETE [FROM] target_name [[AS] alias] WHERE boolean_expression`. Placing an alias token between `DELETE` and `FROM` (e.g., `DELETE ALIAS FROM target_name ALIAS ...`) causes a syntax error.
  - Adding `and _databaseId != 'spanner'` to the negative dialect conditionals ensures MyBatis evaluates to false when `_databaseId == 'spanner'`, emitting:
    ```sql
    delete from <TABLE_NAME> <ALIAS> where <ALIAS>.<COL> is not null and <ALIAS>.<COL> != '' and NOT EXISTS (...)
    ```
  - The table alias after `<TABLE_NAME>` is preserved, allowing correlated subqueries (e.g. `where NOT EXISTS (select ... from ... where <ALIAS>.ID_ = ...)`) to resolve properly.
- **Mandatory WHERE Clause Compliance (`GSQL-C7`)**:
  - Every modified bulk delete statement contains an unconditional `WHERE` clause with non-null and `NOT EXISTS` subquery predicates.
  - Tenant updates in [`ChangeTenantBpmn.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/ChangeTenantBpmn.xml) include `<include refid="changeTenantIdProcessCriteriaSql"/>`, ensuring a valid `WHERE` clause is always present.
- **Typing and Parameters (`GSQL-C8`)**:
  - Tenant update statements specify explicit `jdbcType=VARCHAR`, preventing untyped `NULL` parameter inference issues.
- **Preservation of Legacy Dialects**:
  - For MySQL and SQL Server, `_databaseId != 'spanner'` evaluates to true, so the legacy alias syntax (`DELETE <ALIAS> FROM ...`) is retained.
  - PostgreSQL, CockroachDB, and DB2 continue to omit the alias.
  - Database-specific statement overrides (`databaseId="oracle"`, `databaseId="h2"`, `databaseId="hsql"`) remain unaltered.

### 2. Completeness: PASS

- **Plan Scope Adherence (`.sam/plan.md`)**:
  - All target files designated in Unit 4 of the migration plan were audited and updated:
    - [`HistoricDetail.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricDetail.xml#L219)
    - [`HistoricActivityInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricActivityInstance.xml#L288)
    - [`HistoricVariableInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml#L291) & [L327](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml#L327)
    - [`HistoricTaskLogEntry.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml#L223) & [L238](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml#L238)
    - [`ChangeTenantBpmn.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/ChangeTenantBpmn.xml#L28)
- **Extended Cross-Module Coverage**:
  - Audited and updated corresponding statements in all other Flowable engines and services:
    - [`HistoricTaskInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskInstance.xml#L389) & [L409](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskInstance.xml#L409)
    - [`HistoricIdentityLink.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-identitylink-service/src/main/resources/org/flowable/identitylink/service/db/mapping/entity/HistoricIdentityLink.xml#L137), [L157](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-identitylink-service/src/main/resources/org/flowable/identitylink/service/db/mapping/entity/HistoricIdentityLink.xml#L157), & [L177](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-identitylink-service/src/main/resources/org/flowable/identitylink/service/db/mapping/entity/HistoricIdentityLink.xml#L177)
    - [`HistoricMilestoneInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricMilestoneInstance.xml#L93)
    - [`HistoricPlanItemInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricPlanItemInstance.xml#L280)
- **Zero Omissions**:
  - Exhaustive scan across all 136 MyBatis XML mapper files confirmed exactly 11 statements utilized the negative conditional pattern; all 11 have been updated with `and _databaseId != 'spanner'`.
  - All other delete statements with dialect conditionals use positive checks (`_databaseId == 'mysql' or _databaseId == 'mssql'`), which evaluate to false for Spanner.
  - Zero TODOs, stubs, or unresolved changes exist.

### 3. Test Integrity: PASS

- **Genuine Execution Against MyBatis Configuration**:
  - Test suite [`SpannerMyBatisMapperTest`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerMyBatisMapperTest.java) initializes real Flowable `ProcessEngineConfiguration` and MyBatis `Configuration` instances with `databaseType = "spanner"` and `databaseType = "mysql"`.
  - Exercises actual MyBatis AST parsing and dynamic SQL evaluation via `BoundSql.getSql()` across real production XML mapping files.
- **Rigor of Assertions**:
  - 11 parameterized tests verify that for Spanner, SQL starts with `delete from` and does not match the illegal alias pattern `(?s)(?i)^\s*delete\s+(?!from\b)[a-z0-9_]+\s+from.*`.
  - 11 parameterized tests verify that for MySQL, legacy alias syntax is strictly maintained.
  - Validates `changeTenantIdExecutions` for GoogleSQL UPDATE syntax, presence of `WHERE`, and absence of MSSQL-specific clauses.
- **Zero Weakened or Neutralized Tests**:
  - No tests were skipped or disabled.
  - All 23 tests pass cleanly.
  - All existing reactor tests matching `*Mapper*` and `*Spanner*` run and pass cleanly (35 tests total).

---

## Verification Evidence
```bash
# 1. Spanner mapper unit test suite
./mvnw test -pl modules/flowable-engine -am -Dtest=SpannerMyBatisMapperTest -Dsurefire.failIfNoSpecifiedTests=false
# Results: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.961 s

# 2. Reactor-wide mapper and Spanner tests
./mvnw test -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service -am -Dtest=*Mapper*,*Spanner* -Dsurefire.failIfNoSpecifiedTests=false
# Results: Tests run: 35, Failures: 0, Errors: 0, Skipped: 0

# 3. Checkstyle code quality audit
./mvnw checkstyle:check -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service,modules/flowable-identitylink-service,modules/flowable-cmmn-engine
# Results: 0 Checkstyle violations
```
