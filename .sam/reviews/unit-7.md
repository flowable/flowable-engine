---
unit: unit-7
round: 3
status: accept
dimensions:
  correctness: pass
  completeness: pass
  test-integrity: pass
evidence: |
  Real-target execution against Cloud Spanner Emulator (localhost:9010):
  Two consecutive full reactor test runs passed cleanly with 0 failures and 0 errors:
  Command: ./mvnw test -Pspanner -pl modules/flowable-engine-common,modules/flowable-engine -am -Dtest=*Spanner* -Dsurefire.failIfNoSpecifiedTests=false
  - Run 1: 44/44 reactor modules SUCCESS; 33/33 tests passed in flowable-engine, 22/22 tests passed in flowable-engine-common (Total time: 54.59s).
  - Run 2: 44/44 reactor modules SUCCESS; 33/33 tests passed in flowable-engine, 22/22 tests passed in flowable-engine-common (Total time: 56.55s).
  - Checkstyle audit: 0 violations across modified modules.
---

# Review Assessment: Unit 7 (Verification & Test Harness for Cloud Spanner) — Round 3

## Executive Summary

Work Unit 7 has successfully established the Cloud Spanner verification harness, automated GitHub Actions CI workflow, and integration test suite for Flowable Process Engine.

In Round 3 (Rework), all previously identified defects in DDL lifecycle teardown, error handling, and repeatable execution on Cloud Spanner have been fully remediated:
1. **Missing-Constraint Suppression (`AbstractSqlScriptBasedDbSchemaManager.java`)**: `isSpannerNotFoundException()` intercepts and suppresses `NOT_FOUND` errors during Spanner schema drop operations, accommodating GoogleSQL's lack of `DROP CONSTRAINT IF EXISTS`.
2. **Decoupled Drop Blocks (`ProcessDbSchemaManager.java`)**: `drop engine` and `drop history` execute in separate `try-catch` blocks, ensuring history teardown is never skipped if engine teardown encounters warnings.
3. **Defensive History Index Drops (`flowable.spanner.drop.common.sql`)**: Explicit `DROP INDEX IF EXISTS` for all secondary history indices on common tables (`ACT_IDX_HI_IDENT_LNK_TASK`, `ACT_IDX_HI_IDENT_LNK_PROCINST`, `ACT_IDX_HI_TASK_INST_PROCINST`, `ACT_IDX_HI_PROCVAR_*`) guarantees clean table teardown complying with Spanner's rule prohibiting dropping tables with active indices (`GSQL-V3`).
4. **Consecutive Verification**: Two consecutive full test runs against the active Cloud Spanner Emulator container executed with 100% success (33/33 tests in `flowable-engine`, 22/22 tests in `flowable-engine-common`, 0 failures, 0 errors, 0 skipped).

Overall status is **ACCEPT**.

---

## Dimension Breakdown

### 1. Completeness: PASS
- **Scope Delivery**:
  - Test configuration template in [`modules/flowable-engine/src/test/resources/flowable.spanner.cfg.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/resources/flowable.spanner.cfg.xml) fully configures HikariCP `ClosingDataSource`, Spanner JDBC driver, `databaseType="spanner"`, `databaseSchemaUpdate="drop-create"`, and `StrongUuidGenerator`.
  - CI workflow in [`.github/workflows/spanner.yml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/.github/workflows/spanner.yml) configures `gcr.io/cloud-spanner-emulator/emulator:latest` service container (ports 9010 & 9020), Java 17 Temurin, maven caching, and Maven test execution with `-Pspanner`, `;lenient=true;`, and `-Dsurefire.failIfNoSpecifiedTests=false`.
  - Integration test harness in [`modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerEngineIntegrationTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/java/org/flowable/engine/test/db/SpannerEngineIntegrationTest.java) verifies configuration bean parsing and end-to-end process lifecycle execution.
  - Lifecycle cleanup in [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/test/ClosingDataSource.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/test/ClosingDataSource.java) implements `AutoCloseable` delegating to `onEngineClosed(null)`.
  - Teardown robustness in [`AbstractSqlScriptBasedDbSchemaManager.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java), [`ProcessDbSchemaManager.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/java/org/flowable/engine/impl/db/ProcessDbSchemaManager.java), and [`flowable.spanner.drop.common.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/resources/org/flowable/common/db/drop/flowable.spanner.drop.common.sql).
- **Code Hygiene**: Zero `TODO`, `FIXME`, stubbed methods, or placeholder comments.
- **Build Quality**: Full compile clean, 0 Checkstyle violations in modified modules.

---

### 2. Correctness: PASS
- **Resolution of Previous Findings**:
  - Finding 1 (JDBC URL options): Appended `;lenient=true;` in `.github/workflows/spanner.yml` and `flowable.spanner.cfg.xml` (`JDBC-V1`).
  - Finding 2 (Reactor surefire flag): Added `-Dsurefire.failIfNoSpecifiedTests=false` to Maven test execution under `-am`.
  - Finding 3 (Concurrent DDL collisions): Handled with exponential backoff retry loop in `AbstractSqlScriptBasedDbSchemaManager.java` (`JDBC-V5`).
  - Finding 4 (DataSource resource leak): Enclosed in `try ... finally` block closing `configuration.getDataSource()` in `SpannerEngineIntegrationTest.java`.
  - Finding 5 (Teardown drop constraint and index dependencies): Addressed by ignoring `NOT_FOUND` errors during drop operations (`AbstractSqlScriptBasedDbSchemaManager.java`), isolating history drop execution (`ProcessDbSchemaManager.java`), and adding defensive index drops in `flowable.spanner.drop.common.sql` (`GSQL-V3`, `JDBC-V5`).
- **Validation Rules Compliance**:
  - `SPN-V1`: Verified transaction idempotency under retry; repeated test runs execute cleanly without residual state conflicts.
  - `SPN-V2`: Join depth is well within the 20-table limit.
  - `SPN-V3`: Uses `StrongUuidGenerator` to prevent primary key sequential hotspotting; no `SKIP LOCKED` reliance.
  - `JDBC-V1` – `JDBC-V8`: JDBC URL correctly configured with lenient flag; aborts and concurrency handled via exception hierarchy rather than SQLState; DDL scripts enforce autocommit; `SERIALIZABLE` isolation preserved.
  - `GSQL-V1` – `GSQL-V3`: Explicit bounds on `STRING`/`BYTES`; child tables and secondary indices dropped before parent tables.

---

### 3. Test-Integrity: PASS
- **Positive Verification Evidence**:
  - Real-target execution verified against active Cloud Spanner emulator container on `localhost:9010`.
  - Full end-to-end BPMN 2.0 lifecycle executed:
    - Schema creation via `drop-create`.
    - Deployment of `spannerSampleProcess.bpmn20.xml` with `RepositoryService`.
    - Process instance start with UUID keys via `RuntimeService`.
    - User Task (`Review Task`) completion via `TaskService`.
    - Completion verified (`count() == 0`).
    - Historic process and task instance persistence validated with non-null end times via `HistoryService`.
    - Clean engine closure via `processEngine.close()`.
- **Repeatability & Idempotency Verified**:
  - Successfully executed back-to-back full test runs:
    `./mvnw test -Pspanner -pl modules/flowable-engine-common,modules/flowable-engine -am -Dtest=*Spanner* -Dsurefire.failIfNoSpecifiedTests=false`
    - Run 1: 55/55 Spanner tests passed, 0 failures, 0 errors.
    - Run 2: 55/55 Spanner tests passed, 0 failures, 0 errors.
- **Test Integrity Protection**:
  - Guarded with JUnit 5 `assumeTrue` checks (`isSpannerDriverPresent()` and `isSpannerEmulatorReachable()`), ensuring non-Spanner builds skip cleanly while Spanner CI builds actively exercise the emulator.
  - Zero tests weakened, neutralized, or deleted.

---

## Verdict

**ACCEPT**. Work Unit 7 meets all requirements across Correctness, Completeness, and Test-Integrity.
