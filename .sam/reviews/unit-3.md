---
unit: unit-3
round: 1
status: accept
dimensions:
  correctness: pass
  completeness: pass
  test-integrity: pass
evidence: |
  1. ./mvnw test -pl modules/flowable-engine-common -Dtest=SpannerRetryInterceptorTest -> 14 run, 0 failures, 0 errors, 0 skipped (BUILD SUCCESS)
  2. ./mvnw test -pl modules/flowable-engine -Dtest=SpannerProcessEngineConfigurationTest -> 3 run, 0 failures, 0 errors, 0 skipped (BUILD SUCCESS)
  3. ./mvnw checkstyle:check -pl modules/flowable-engine-common,modules/flowable-engine -> 0 Checkstyle violations (BUILD SUCCESS)
---

# Review Assessment: Unit 3 - Transaction Abort Handling & Spanner Retry Interceptor

## Executive Summary
Unit 3 implements Cloud Spanner transaction abort retry handling via `SpannerRetryInterceptor` and defaults the primary key generator for `ProcessEngineConfigurationImpl` to `StrongUuidGenerator` when running on Cloud Spanner. The changes have been reviewed across the three mandatory dimensions (**Correctness**, **Completeness**, **Test-Integrity**), each independently assessed by dedicated sub-reviewers. All dimensions **PASS**. The implementation is accepted without rework.

---

## Dimension Assessments

### 1. Correctness: PASS
- **Spanner Abort Classification (`JDBC-V2`, `JDBC-V4`)**:
  - `SpannerRetryInterceptor` properly inspects Spanner-specific abort markers without relying on `SQLState` (which is `null` in Cloud Spanner JDBC driver).
  - Matches class names `AbortedException` and `AbortedDueToConcurrentModificationException`.
  - Matches gRPC status code ordinal 10 (`Status.Code.ABORTED`) via `SQLException.getErrorCode() == 10`.
  - Matches error message text containing `"ABORTED"`.
  - Fully traverses nested causes (`getCause()`) and chained JDBC exceptions (`SQLException.getNextException()`), incorporating cycle detection via a `Set<Throwable> visited` guard to prevent stack overflow.
- **Fail-Fast and Thread Interruption**:
  - Non-retryable exceptions immediately rethrow on the first execution attempt without futile sleeping or retrying.
  - `waitBeforeRetry` catches `InterruptedException` and properly restores the interrupt status with `Thread.currentThread().interrupt()`.
- **Command Interceptor Ordering (`SPN-V1`)**:
  - Registered immediately before `createTransactionInterceptor()` in `AbstractEngineConfiguration.getDefaultCommandInterceptors()`. When a transaction abort occurs during commit or execution, the inner transaction rolls back and the exception propagates to `SpannerRetryInterceptor`, which retries the command outside the transaction boundary with exponential backoff, initiating a clean, fresh transaction.
  - Dialect gating is strict: `else if (DATABASE_TYPE_SPANNER.equals(databaseType))`, leaving H2, CockroachDB, MySQL, PostgreSQL, DB2, and Oracle behavior completely untouched.
- **Identifier Generation on Spanner (`SPN-D7`, `GSQL-C1`)**:
  - In `ProcessEngineConfigurationImpl.initIdGenerator()`, when `DATABASE_TYPE_SPANNER.equals(databaseType)` and `idGenerator == null`, defaults to `StrongUuidGenerator`.
  - This eliminates monotonic sequential integer block allocation (`DbIdGenerator`) on Spanner, preventing write hotspotting on root split ranges.
  - Preserves user-configured custom `IdGenerator` instances and preserves `DbIdGenerator` for non-Spanner databases.

### 2. Completeness: PASS
- **Scope Verification against `.sam/plan.md`**:
  - `SpannerRetryInterceptor.java` authored with configurable `nrRetries = 3`, `waitTime = 50ms`, `waitTimeIncrease = 2`, along with full getters and setters.
  - `AbstractEngineConfiguration.java` registered `SpannerRetryInterceptor` into the command interceptor chain for `DATABASE_TYPE_SPANNER`.
  - `ProcessEngineConfigurationImpl.java` configured `StrongUuidGenerator` default for Spanner.
  - Sub-engine parity verified: other Flowable engines (CMMN, DMN, IDM, App, Event Registry) inherit `initIdGenerator()` from `AbstractEngineConfiguration`, which already defaults to `StrongUuidGenerator`.
- **No Stubs or Omissions**:
  - Zero `TODO`, `FIXME`, or stub markers across all touched production and test source files.
  - Checkstyle completed with 0 violations across both `flowable-engine-common` and `flowable-engine`.

### 3. Test-Integrity: PASS
- **Substantive Behavioral Verification**:
  - `SpannerRetryInterceptorTest` (14 unit tests) validates:
    - Normal single-pass execution.
    - Simulated abort with retry loop and eventual success.
    - Retry exhaustion throwing the original exception.
    - Immediate rethrow on non-retryable exceptions.
    - Abort detection by gRPC code 10, `"ABORTED"` message text, and Spanner exception class names.
    - Nested cause chain and `SQLException.getNextException()` recursion.
    - Cycle protection during exception traversal.
    - Configuration property getters and setters.
    - Presence in `AbstractEngineConfiguration` interceptor chain for Spanner and absence for H2.
  - `SpannerProcessEngineConfigurationTest` (3 unit tests) validates:
    - Defaulting to `StrongUuidGenerator` on Spanner.
    - Defaulting to `DbIdGenerator` on H2.
    - Preservation of custom `IdGenerator` on Spanner.
- **No Neutralization or Test Weakening**:
  - Zero `@Disabled` or `@Ignore` annotations.
  - Zero vacuous or trivial assertions (`assertTrue(true)`).
  - No pre-existing tests were modified or deleted.
- **Execution Evidence**:
  - `./mvnw test -pl modules/flowable-engine-common -Dtest=SpannerRetryInterceptorTest`: 14 tests run, 0 failures, 0 errors, 0 skipped (BUILD SUCCESS).
  - `./mvnw test -pl modules/flowable-engine -Dtest=SpannerProcessEngineConfigurationTest`: 3 tests run, 0 failures, 0 errors, 0 skipped (BUILD SUCCESS).
  - `./mvnw test -pl modules/flowable-engine-common,modules/flowable-engine -am -Dtest=*Spanner*,*Retry* -Dsurefire.failIfNoSpecifiedTests=false`: All 29 tests run, 0 failures, 0 errors (BUILD SUCCESS).

---

## Conclusion
Unit 3 satisfies all correctness, completeness, and test-integrity requirements. The unit is accepted.
