# Unit 3: Transaction Abort Handling & Spanner Retry Interceptor - Change Summary

## Overview
Implemented Cloud Spanner transaction abort retry handling via `SpannerRetryInterceptor` and configured `StrongUuidGenerator` as the default identifier generator for `ProcessEngineConfigurationImpl` when using Cloud Spanner, avoiding primary key write hotspotting on Spanner root splits.

## Modified and Created Files
- [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/interceptor/SpannerRetryInterceptor.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/interceptor/SpannerRetryInterceptor.java) *(New File)*
- [`modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java)
- [`modules/flowable-engine/src/main/java/org/flowable/engine/impl/cfg/ProcessEngineConfigurationImpl.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/main/java/org/flowable/engine/impl/cfg/ProcessEngineConfigurationImpl.java)
- [`modules/flowable-engine-common/src/test/java/org/flowable/common/engine/impl/interceptor/SpannerRetryInterceptorTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine-common/src/test/java/org/flowable/common/engine/impl/interceptor/SpannerRetryInterceptorTest.java) *(New File)*
- [`modules/flowable-engine/src/test/java/org/flowable/engine/test/cfg/SpannerProcessEngineConfigurationTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-engine/src/test/java/org/flowable/engine/test/cfg/SpannerProcessEngineConfigurationTest.java) *(New File)*

## Details of Changes
1. **`SpannerRetryInterceptor.java`**:
   - Modeled after `CrDbRetryInterceptor` with configurable retry count (`nrRetries = 3`), initial wait time (`waitTime = 50ms`), exponential multiplier (`waitTimeIncrease = 2`), and corresponding getters and setters.
   - Implemented `isTransactionRetryException(Throwable exception)` with recursive cause-chain inspection, `SQLException.getNextException()` traversal, and cycle protection:
     - Identifies Spanner abort indicators via exception class names (`AbortedException`, `AbortedDueToConcurrentModificationException`).
     - Detects gRPC status code 10 (`ABORTED`) on `SQLException.getErrorCode()`.
     - Detects message content containing `"ABORTED"`.
     - Intentionally avoids `SQLState` matching because Cloud Spanner JDBC driver reports `null` for `SQLState` (`JDBC-C2`).
     - Re-throws non-retryable exceptions immediately on the first attempt without unnecessary retries.
2. **`AbstractEngineConfiguration.java`**:
   - In `getDefaultCommandInterceptors()`, registered `SpannerRetryInterceptor` into the command interceptor chain when `DATABASE_TYPE_SPANNER.equals(databaseType)`.
3. **`ProcessEngineConfigurationImpl.java`**:
   - In `initIdGenerator()`, defaulted to `idGenerator = new StrongUuidGenerator()` when `DATABASE_TYPE_SPANNER.equals(databaseType)`.
   - Preserves custom user-provided `IdGenerator` instances when configured.
   - Prevents sequential integer block allocation (`DbIdGenerator`) from creating write hotspots on Spanner's split boundary root ranges (`GSQL-C1`, `SPN-D7`).

## Decisions & Defaults
- **Exception Classification by gRPC Ordinal and Status Name**: In accordance with `JDBC-C2` and `JDBC-C4`, Cloud Spanner errors surface as `JdbcSqlException` wrapping `SpannerException` with null `SQLState` and gRPC status ordinal (10 for `ABORTED`). `isTransactionRetryException` matches error code 10, `"ABORTED"` message text, and Spanner exception class names rather than SQLState 40001.
- **Backoff Configuration**: Defaulted to `nrRetries = 3`, `waitTime = 50ms`, `waitTimeIncrease = 2` with full programmatic access via setters/getters.
- **UUID Primary Key Generation**: `StrongUuidGenerator` was adopted as the default ID generator for `ProcessEngine` on Spanner, bringing it into parity with all other Flowable engines (CMMN, DMN, Form, IDM, App, Event Registry) which already use `StrongUuidGenerator` by default via `AbstractEngineConfiguration`.

## Verification & Test Status
- Authored unit test suites:
  - `SpannerRetryInterceptorTest` in `modules/flowable-engine-common`: 14 tests verifying retry on simulated Spanner abort exceptions, exponential backoff, retry exhaustion, immediate re-throw of non-retryable exceptions, full cause chain and `SQLException.getNextException()` traversal, property getters/setters, and interceptor chain inclusion.
  - `SpannerProcessEngineConfigurationTest` in `modules/flowable-engine`: 3 tests verifying `StrongUuidGenerator` default on Spanner, `DbIdGenerator` default on H2, and preservation of custom `IdGenerator` on Spanner.
- TDD cycle:
  - Red phase: test compilation failed due to missing `SpannerRetryInterceptor` class in `flowable-engine-common`, and runtime error confirmed `DbIdGenerator` failure on Spanner in `flowable-engine`.
  - Green phase: all tests passed cleanly following implementation.
- Executed tests:
  - `./mvnw test -pl modules/flowable-engine-common,modules/flowable-engine -am -Dtest=*Spanner*,*Retry* -Dsurefire.failIfNoSpecifiedTests=false`: All 10 tests passed (0 failures, 0 errors).
- Executed Checkstyle:
  - `./mvnw checkstyle:check -pl modules/flowable-engine-common,modules/flowable-engine`: 0 Checkstyle violations in both modules.
