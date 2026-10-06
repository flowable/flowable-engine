# Unit 4: MyBatis Mapper Adjustments for GoogleSQL Syntax Compatibility - Change Summary

## Overview
Modified MyBatis XML mappers across Flowable Core, Task Service, Variable Service, Identity Link Service, and CMMN Engine to eliminate unsupported table aliases between `DELETE` and `FROM` in dynamic SQL statements when running on Cloud Spanner (GoogleSQL dialect), and verified tenant update statements in `ChangeTenantBpmn.xml` for GoogleSQL compatibility.

## Modified and Created Files
- [`modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricDetail.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricDetail.xml)
- [`modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricActivityInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/resources/org/flowable/db/mapping/entity/HistoricActivityInstance.xml)
- [`modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-variable-service/src/main/resources/org/flowable/variable/service/db/mapping/entity/HistoricVariableInstance.xml)
- [`modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskLogEntry.xml)
- [`modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-task-service/src/main/resources/org/flowable/task/service/db/mapping/entity/HistoricTaskInstance.xml)
- [`modules/flowable-identitylink-service/src/main/resources/org/flowable/identitylink/service/db/mapping/entity/HistoricIdentityLink.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-identitylink-service/src/main/resources/org/flowable/identitylink/service/db/mapping/entity/HistoricIdentityLink.xml)
- [`modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricMilestoneInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricMilestoneInstance.xml)
- [`modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricPlanItemInstance.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/mapping/entity/HistoricPlanItemInstance.xml)
- [`modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerMyBatisMapperTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerMyBatisMapperTest.java) *(New File)*

## Details of Changes
1. **GoogleSQL DELETE Alias Elimination**:
   - GoogleSQL DML DELETE syntax is `DELETE [FROM] target_name [[AS] alias] WHERE boolean_expression`. Placing an alias immediately between `DELETE` and `FROM` (e.g. `DELETE ALIAS FROM target_name ALIAS ...`, which MySQL supports) causes a GoogleSQL syntax error (`GSQL-C7`).
   - Updated the negative dialect conditional `<if test="_databaseId != 'postgres' and _databaseId != 'cockroachdb' and _databaseId != 'db2'">` to append `and _databaseId != 'spanner'` across all affected mapper statements:
     - `HistoricDetail.xml`: `bulkDeleteHistoricDetailForNonExistingProcessInstances` (line 219)
     - `HistoricActivityInstance.xml`: `bulkDeleteHistoricActivityInstancesForNonExistingProcessInstances` (line 288)
     - `HistoricVariableInstance.xml`: `bulkDeleteHistoricVariableInstancesForNonExistingProcessInstances` (line 291) and `bulkDeleteHistoricVariableInstancesForNonExistingCaseInstances` (line 327)
     - `HistoricTaskLogEntry.xml`: `bulkDeleteHistoricTaskLogEntriesForNonExistingProcessInstances` (line 223) and `bulkDeleteHistoricTaskLogEntriesForNonExistingCaseInstances` (line 238)
     - `HistoricTaskInstance.xml`: `bulkDeleteHistoricTaskInstancesForNonExistingProcessInstances` (line 389) and `bulkDeleteHistoricTaskInstancesForNonExistingCaseInstances` (line 409)
     - `HistoricIdentityLink.xml`: `bulkDeleteHistoricProcessIdentityLinks` (line 137), `bulkDeleteHistoricCaseIdentityLinks` (line 157), and `bulkDeleteHistoricTaskIdentityLinks` (line 177)
     - `HistoricMilestoneInstance.xml`: `bulkDeleteHistoricMilestoneInstancesForNonExistingCaseInstances` (line 93)
     - `HistoricPlanItemInstance.xml`: `bulkDeleteHistoricPlanItemInstancesForNonExistingCaseInstances` (line 280)
2. **Tenant Update Verification (`ChangeTenantBpmn.xml`)**:
   - Inspected tenant update statements (`changeTenantIdExecutions`, `changeTenantIdActivityInstances`, etc.).
   - Confirmed statements adhere to GoogleSQL syntax `UPDATE target_name [alias] SET ... WHERE ...`.
   - Verified that `WHERE` criteria is unconditionally included (`changeTenantIdProcessCriteriaSql`), satisfying GoogleSQL mandatory `WHERE` clause constraint (`GSQL-C7`).

## Decisions & Defaults
- **Exclusion of Spanner alongside PostgreSQL/CockroachDB/DB2**: Rather than introducing Spanner-specific statement overrides, Spanner was cleanly added to the existing `_databaseId != 'spanner'` conditional check. This emits clean `DELETE FROM table alias WHERE ...` statements without code duplication.
- **Exclusion Scope Across All Engine Mappers**: Extended the fix beyond Process Engine history to Task, Variable, IdentityLink, and CMMN engines, ensuring consistent GoogleSQL DML generation across all sub-engine cleanup jobs.
- **Preservation of Legacy Dialects**: Verified that MySQL and other non-PostgreSQL dialects continue to emit their native alias syntax without regression.

## Verification & Test Status
- Authored unit test suite:
  - `SpannerMyBatisMapperTest` in `modules/flowable-engine`:
    - 11 parameterized tests verifying that when `_databaseId == "spanner"`, generated SQL for all bulk delete statements starts with `delete from` and does not include table aliases between `DELETE` and `FROM`.
    - 11 parameterized tests verifying that when `_databaseId == "mysql"`, legacy syntax (`DELETE <alias> FROM ...`) is strictly preserved.
    - Verified `ChangeTenantBpmn.xml` statement `changeTenantIdExecutions` for GoogleSQL compatibility and presence of WHERE clause.
- TDD cycle:
  - **Red phase**: Ran `SpannerMyBatisMapperTest` before modifying XML mappers; all 11 Spanner tests failed with `AssertionError`, demonstrating the syntax defect.
  - **Green phase**: Ran `SpannerMyBatisMapperTest` after modifying XML mappers; all 23 tests passed cleanly.
- Build & Test execution:
  - `./mvnw test-compile -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service -am`: `BUILD SUCCESS`.
  - `./mvnw test -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service -am -Dtest=*Mapper*,*Spanner* -Dsurefire.failIfNoSpecifiedTests=false`: 35 tests run, 0 failures, 0 errors across reactor.
- Checkstyle audit:
  - `./mvnw checkstyle:check -pl modules/flowable-engine,modules/flowable-variable-service,modules/flowable-task-service`: 0 Checkstyle violations.
  - `./mvnw checkstyle:check -pl modules/flowable-identitylink-service,modules/flowable-cmmn-engine`: 0 Checkstyle violations.
