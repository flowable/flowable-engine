# Unit 7: Verification & Test Harness for Cloud Spanner - Change Summary

## Overview
Unit 7 establishes the Cloud Spanner test configuration template, automated GitHub Actions CI workflow with the Cloud Spanner Emulator, and end-to-end integration test harness for the Flowable Process Engine.

## Target Files

### Configuration Templates & CI
1. [`modules/flowable-engine/src/test/resources/flowable.spanner.cfg.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/resources/flowable.spanner.cfg.xml) *(New File)*:
   - Configures `ClosingDataSource` wrapping HikariCP connection pool with Spanner JDBC URL (`jdbc:cloudspanner://localhost:9010/projects/test-project/instances/test-instance/databases/test-db;usePVGeneration=true;lenient=true;autoConfigEmulator=true`) and driver `com.google.cloud.spanner.jdbc.JdbcDriver`.
   - Configures `StandaloneProcessEngineConfiguration` with `databaseType="spanner"`, `databaseSchemaUpdate="drop-create"`, and `StrongUuidGenerator`.
2. [`.github/workflows/spanner.yml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/.github/workflows/spanner.yml) *(New File)*:
   - Defines GitHub Actions CI workflow running on push and pull requests for `main` and `spanner-migration` branches.
   - Spins up `gcr.io/cloud-spanner-emulator/emulator:latest` service container exposing ports 9010 (gRPC) and 9020 (REST).
   - Sets up Java 17 (Eclipse Temurin) and executes Maven tests with `-Pspanner`, `-Dsurefire.failIfNoSpecifiedTests=false`, and Spanner emulator parameters with `;lenient=true;`.

### Integration Tests
3. [`modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerEngineIntegrationTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerEngineIntegrationTest.java) *(New File)*:
   - `testFlowableSpannerConfigurationParsing`: Validates Spring bean parsing of `flowable.spanner.cfg.xml`, asserting `databaseType="spanner"`, `databaseSchemaUpdate="drop-create"`, `StrongUuidGenerator`, `SpannerRetryInterceptor` inclusion in the command interceptor chain, and Spanner JDBC driver properties. Wraps execution in `try ... finally` and closes `configuration.getDataSource()` if `AutoCloseable` to prevent pool connection leaks across tests.
   - `testSpannerEngineAgainstEmulator`: Full end-to-end test against the Cloud Spanner Emulator:
     - Boots `ProcessEngine` and applies schema via `drop-create`.
     - Programmatically constructs and deploys a sample BPMN 2.0 process (`spannerSampleProcess`) containing a User Task.
     - Starts a process instance and verifies its active execution.
     - Queries and completes the User Task (`Review Task`).
     - Verifies process instance completion.
     - Queries and asserts historic data persistence (`HistoricProcessInstance` and `HistoricTaskInstance`).
     - Performs clean engine teardown via `processEngine.close()`.
   - Graceful fallback: Uses JUnit 5 `assumeTrue` checks for both driver presence on classpath (requiring `-Pspanner`) and emulator TCP reachability on `localhost:9010`.

### Core Engine & Schema Management Alignment
4. [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/test/ClosingDataSource.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/test/ClosingDataSource.java):
   - Implemented `AutoCloseable` with `close()` method delegating to `onEngineClosed(null)`.
   - Broadened wrapped `dataSource` inspection in `onEngineClosed` to `AutoCloseable` to safely close non-`Closeable` pools like HikariCP.
5. [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java):
   - Aligned DDL execution with Cloud Spanner's transaction model ([`JDBC-C5`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L78)): Cloud Spanner rejects DDL statements executed inside a multi-statement transaction (`autoCommit = false`) with `FAILED_PRECONDITION: DDL-statements are not allowed inside a read/write transaction`.
   - In `executeSchemaResource()`, when `databaseType` is `spanner`, the initial `autoCommit` state is saved and temporarily switched to `true` (`connection.setAutoCommit(true)`) so DDL statements execute in autocommit mode. The previous `autoCommit` setting is restored in a `finally` block before returning the connection to the MyBatis session.
   - Added `isSpannerConcurrentSchemaChange()` collision detection and retry loop with exponential backoff (`200ms * retryCount`, up to 5 attempts) to prevent sequential DDL collisions when rapid schema operations are in progress (`JDBC-V5`).
   - Added `isSpannerNotFoundException()` handling during drop operations (`"drop".equals(operation)` and `"spanner".equals(databaseType)`): ignores `NOT_FOUND` errors (e.g. "is not a constraint in", "Table not found", code NOT_FOUND) instead of setting `exception = e`, since GoogleSQL does not support `ALTER TABLE ... DROP CONSTRAINT IF EXISTS`.
6. [`modules/flowable-engine/src/main/java/org/flowable/engine/impl/db/ProcessDbSchemaManager.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/java/org/flowable/engine/impl/db/ProcessDbSchemaManager.java):
   - Separated `executeMandatorySchemaResource("drop", "engine")` and `executeMandatorySchemaResource("drop", "history")` in `schemaDrop()` into independent `try-catch` blocks so an issue in engine drop never skips history table drop.
7. [`modules/flowable-engine-common/src/main/resources/org/flowable/common/db/drop/flowable.spanner.drop.common.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/resources/org/flowable/common/db/drop/flowable.spanner.drop.common.sql):
   - Defensively added `DROP INDEX IF EXISTS` for history indices created on common tables (`ACT_IDX_HI_IDENT_LNK_TASK`, `ACT_IDX_HI_IDENT_LNK_PROCINST`, `ACT_IDX_HI_TASK_INST_PROCINST`, `ACT_IDX_HI_PROCVAR_PROC_INST`, `ACT_IDX_HI_PROCVAR_TASK_ID`, `ACT_IDX_HI_PROCVAR_EXE`) before dropping tables.

## Decisions & Defaults
- **Autocommit for Spanner DDL ([`JDBC-C5`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/access/spanner-jdbc/conversion.md#L78))**: Flowable's `DbSqlSession` binds JDBC connections with `autoCommit = false`. For Spanner, DDL statements must execute in autocommit mode. Toggling `autoCommit` during schema operations preserves MyBatis transaction semantics for application DML while allowing DDL creation and teardown scripts to run seamlessly.
- **Teardown Ordering in Tests**: `processEngine.close()` cleans up active engine state before tables are dropped, ensuring clean, error-free shutdown.
- **Graceful Fallback**: Test suite gracefully skips emulator execution if the Spanner JDBC driver is not on the classpath or if the emulator port 9010 is not running, ensuring `mvn test` without `-Pspanner` continues to build cleanly.

## Reviewer Rework & Teardown Idempotency Actions
1. **CI Workflow URL and Surefire Args**:
   - Appended `;lenient=true;` to `-Djdbc.url` in `.github/workflows/spanner.yml`.
   - Added `-Dsurefire.failIfNoSpecifiedTests=false` to Maven test command in CI so reactor builds with `-am` do not fail on upstream modules lacking Spanner tests.
2. **DataSource Resource Cleanup**:
   - Implemented `AutoCloseable` on `ClosingDataSource` and invoked `close()` inside a `try ... finally` block in `SpannerEngineIntegrationTest.testFlowableSpannerConfigurationParsing()`.
3. **Teardown Idempotency on Spanner**:
   - In `AbstractSqlScriptBasedDbSchemaManager`, ignored `NOT_FOUND` exceptions during drop statements on Spanner (`isSpannerNotFoundException`) so missing constraints do not abort or flag the drop as failed.
   - In `ProcessDbSchemaManager`, separated engine and history drops into separate `try-catch` blocks so an engine drop error never bypasses history drop.
   - In `flowable.spanner.drop.common.sql`, added defensive index drops for history indices on common tables before dropping common tables.
4. **Consecutive Verification Runs**:
   - Successfully executed two consecutive full reactor builds against an active emulator:
     `./mvnw test -Pspanner -pl modules/flowable-engine-common,modules/flowable-engine -am -Dtest=*Spanner* -Dsurefire.failIfNoSpecifiedTests=false`
     - Run 1: 44/44 reactor modules SUCCESS, 33/33 tests passed, 0 failures, 0 errors.
     - Run 2: 44/44 reactor modules SUCCESS, 33/33 tests passed, 0 failures, 0 errors.
5. **Checkstyle Compliance**:
   - Verified 0 Checkstyle violations in both `flowable-engine-common` and `flowable-engine`.
