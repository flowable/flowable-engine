---
unit: unit-6
round: 1
status: accept
dimensions:
  correctness: pass
  completeness: pass
  test-integrity: pass
evidence: Real Cloud Spanner Emulator (localhost:9020 / localhost:9010, GoogleSQL dialect) executed all 282 DDL statements and 9 seed DML statements from the unified distribution script (flowable.spanner.all.create.sql), creating 62 tables, 60 foreign keys, and 219 secondary/unique indexes with 0 errors. Standalone creation, seed property inserts, and teardown drop cycles across all 5 sub-engines (IDM, CMMN, DMN, App, Event Registry) executed with 100% success and 0 residual tables. Maven test suite passed 16/16 tests across all 5 submodules with 0 failures, and Checkstyle passed with 0 violations.
---

# Review Assessment: Unit 6 (Sub-Engines Schema DDL Scripts) — Round 1

## Executive Summary
Unit 6 introduces Cloud Spanner GoogleSQL DDL creation and teardown scripts for all Flowable sub-engines:
- IDM Engine (`flowable-idm-engine`)
- CMMN Engine (`flowable-cmmn-engine`)
- DMN Engine (`flowable-dmn-engine`)
- App Engine (`flowable-app-engine`)
- Event Registry Engine (`flowable-event-registry`)
- Unified distribution script (`distro/sql/create/all/flowable.spanner.all.create.sql`)

All three review dimensions (**Correctness**, **Completeness**, **Test-Integrity**) have been independently verified by dedicated sub-reviewers and unanimously evaluated as **pass**. Real-target lifecycle execution against the Cloud Spanner Emulator verified 100% execution success across all DDL statements, seed property DML inserts, and teardown drop scripts with zero residual database objects.

The unit is approved and accepted.

---

## Dimension Breakdown

### 1. Correctness: `pass`
- **GoogleSQL Dialect & Type System Compliance (`GSQL-V2`, `SPN-D1`, `SPN-D8`)**:
  - All columns across all 30 sub-engine tables strictly use native Cloud Spanner GoogleSQL types (`STRING(n)`, `STRING(MAX)`, `BYTES(MAX)`, `INT64`, `BOOL`, `TIMESTAMP`).
  - No unsupported legacy database types exist (e.g. `VARCHAR`, `TEXT`, `BYTEA`, `BLOB`, `DATETIME`, `BIGINT`, `TINYINT`, `DOUBLE PRECISION`).
  - Unbounded text fields (such as `ACT_DMN_HI_DECISION_EXECUTION.EXECUTION_JSON_`) map correctly to `STRING(MAX)`.
  - Binary payload fields (such as `BYTES_`, `PASSWORD_`, `RESOURCE_BYTES_`) map correctly to `BYTES(MAX)`.
  - Default value expressions adhere to GoogleSQL syntax using parentheses: `DEFAULT ('')` and `DEFAULT (FALSE)`.
- **Primary Key Declarations (`SPN-D4`)**:
  - Every table includes an explicit table-level `PRIMARY KEY (...)` declaration.
  - Conforming to confirmed decision `SPN-D4`, all tables are flat standalone structures without interleaving, maintaining single-column string keys (`ID_ STRING(64)` or `STRING(255)`) or composite keys (`ACT_ID_MEMBERSHIP(USER_ID_, GROUP_ID_)`).
- **Foreign Key Key-Type Symmetry (`GSQL-V3`, `SPN-C2`)**:
  - All 17 foreign key columns match the referenced primary key column definitions in both data type and string length (e.g., `STRING(64)` referencing `STRING(64)`, `STRING(255)` referencing `STRING(255)`).
- **Nullable Unique Indexes (`GSQL-C5`)**:
  - Uniqueness constraints on nullable columns (such as business key, version, and tenant ID) correctly use `CREATE UNIQUE NULL_FILTERED INDEX`:
    - IDM: `ACT_UNIQ_PRIV_NAME` on `ACT_ID_PRIV(NAME_)`
    - CMMN: `ACT_IDX_CASE_DEF_UNIQ` on `ACT_CMMN_CASEDEF(KEY_, VERSION_, TENANT_ID_)`
    - DMN: `ACT_IDX_DMN_DEC_UNIQ` on `ACT_DMN_DECISION(KEY_, VERSION_, TENANT_ID_)`
    - App: `ACT_IDX_APP_DEF_UNIQ` on `ACT_APP_APPDEF(KEY_, VERSION_, TENANT_ID_)`
    - Event Registry: `ACT_IDX_EVENT_DEF_UNIQ` and `ACT_IDX_CHANNEL_DEF_UNIQ`
- **Teardown Syntax & Drop Sequence**:
  - Drop constraints strictly use standard GoogleSQL syntax `ALTER TABLE <table> DROP CONSTRAINT <constraint>;` with no unsupported `IF EXISTS`.
  - Teardown order strictly drops FK constraints first, then indexes via `DROP INDEX IF EXISTS <idx>;`, and finally tables via `DROP TABLE IF EXISTS <tbl>;` in reverse dependency order.
  - 100% 1:1 symmetry between create and drop scripts across all submodules.
- **Real Spanner Emulator Execution**:
  - IDM: 18 DDL create statements, 1 seed insert, 18 DDL drop statements executed with 0 errors.
  - CMMN: 36 DDL create statements, 1 seed insert, 36 DDL drop statements executed with 0 errors.
  - DMN: 8 DDL create statements, 1 seed insert, 8 DDL drop statements executed with 0 errors.
  - App: 8 DDL create statements, 1 seed insert, 8 DDL drop statements executed with 0 errors.
  - Event Registry: 8 DDL create statements, 1 seed insert, 8 DDL drop statements executed with 0 errors.
  - Unified Distro: 282 DDL statements and 9 seed DML statements successfully applied to a clean emulator database with 0 errors.

### 2. Completeness: `pass`
- **Target File Presence (11/11 DDL Scripts, 5/5 Test Classes)**:
  - All assigned production DDL scripts and unit test classes exist and are fully populated:
    - IDM: [`flowable.spanner.create.identity.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/create/flowable.spanner.create.identity.sql), [`flowable.spanner.drop.identity.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/drop/flowable.spanner.drop.identity.sql), [`SpannerIdmDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/test/java/org/flowable/idm/engine/impl/db/SpannerIdmDdlTest.java)
    - CMMN: [`flowable.spanner.create.cmmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/create/flowable.spanner.create.cmmn.sql), [`flowable.spanner.drop.cmmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/drop/flowable.spanner.drop.cmmn.sql), [`SpannerCmmnDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/test/java/org/flowable/cmmn/engine/impl/db/SpannerCmmnDdlTest.java)
    - DMN: [`flowable.spanner.create.dmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/create/flowable.spanner.create.dmn.sql), [`flowable.spanner.drop.dmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/drop/flowable.spanner.drop.dmn.sql), [`SpannerDmnDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/test/java/org/flowable/dmn/engine/impl/db/SpannerDmnDdlTest.java)
    - App: [`flowable.spanner.create.app.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/main/resources/org/flowable/app/db/create/flowable.spanner.create.app.sql), [`flowable.spanner.drop.app.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/main/resources/org/flowable/app/db/drop/flowable.spanner.drop.app.sql), [`SpannerAppDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/test/java/org/flowable/app/engine/impl/db/SpannerAppDdlTest.java)
    - Event Registry: [`flowable.spanner.create.eventregistry.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/create/flowable.spanner.create.eventregistry.sql), [`flowable.spanner.drop.eventregistry.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/drop/flowable.spanner.drop.eventregistry.sql), [`SpannerEventRegistryDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/test/java/org/flowable/eventregistry/impl/db/SpannerEventRegistryDdlTest.java)
    - Unified Distro: [`flowable.spanner.all.create.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/distro/sql/create/all/flowable.spanner.all.create.sql)
- **Table & Column Completeness**:
  - Full schema parity verified against reference PostgreSQL DDL scripts:
    - IDM: 9 tables (`ACT_ID_PROPERTY`, `ACT_ID_BYTEARRAY`, `ACT_ID_GROUP`, `ACT_ID_MEMBERSHIP`, `ACT_ID_USER`, `ACT_ID_INFO`, `ACT_ID_TOKEN`, `ACT_ID_PRIV`, `ACT_ID_PRIV_MAPPING`), 5 secondary indexes, 1 unique null-filtered index, 3 foreign keys.
    - CMMN: 10 tables, 15 secondary indexes, 1 unique null-filtered index, 10 foreign keys.
    - DMN: 4 tables, 2 secondary indexes, 1 unique null-filtered index, 1 foreign key.
    - App Engine: 3 tables, 2 secondary indexes, 1 unique null-filtered index, 2 foreign keys.
    - Event Registry: 4 tables, 1 secondary index, 2 unique null-filtered indexes, 1 foreign key.
- **Unified Distro Script**:
  - Combines all sub-engines in proper topological order: `Common -> Engine Core -> History -> App -> CMMN -> DMN -> Event Registry -> IDM`.
  - 1,575 lines verbatim matching all individual component create scripts.
- **Seed Properties**:
  - All 5 sub-engines insert their required version record: `schema.version` in `ACT_ID_PROPERTY`, and `cmmn.schema.version`, `dmn.schema.version`, `app.schema.version`, `eventregistry.schema.version` in `ACT_GE_PROPERTY`.
- **Code Hygiene**:
  - 0 occurrences of `TODO`, `FIXME`, `STUB`, `XXX`, or temporary placeholders.

### 3. Test-Integrity: `pass`
- **Genuine Assertions**:
  - Unit tests in each sub-engine parse DDL scripts and assert:
    - Exclusion of forbidden legacy types (`varchar`, `bytea`, `longblob`, `tinyint`, `datetime`, etc.).
    - Every table contains table-level `PRIMARY KEY`.
    - Every expected table is defined.
    - Unique indexes use `CREATE UNIQUE NULL_FILTERED INDEX`.
    - Drop scripts strictly omit `DROP CONSTRAINT IF EXISTS`.
    - 1:1 symmetry: every FK constraint and index in create has a matching drop in teardown.
    - Unified distro script parses and validates across all 8 sub-engines.
  - No disabled (`@Disabled`, `@Ignore`), skipped, or empty test methods.
- **Maven Test Suite Results**:
  - Executed `./mvnw test -pl modules/flowable-idm-engine,modules/flowable-cmmn-engine,modules/flowable-dmn-engine,modules/flowable-app-engine,modules/flowable-event-registry -am -Dtest="*Spanner*DdlTest"`:
    - Tests run: 16, Failures: 0, Errors: 0, Skipped: 0. `BUILD SUCCESS`.
- **Checkstyle Audit**:
  - Executed `./mvnw checkstyle:check -pl modules/flowable-idm-engine,modules/flowable-cmmn-engine,modules/flowable-dmn-engine,modules/flowable-app-engine,modules/flowable-event-registry`:
    - 0 violations across all 5 submodules. `BUILD SUCCESS`.
- **Real Cloud Spanner Emulator Verification**:
  - Live execution of 282 DDL statements and 9 seed DML statements from `flowable.spanner.all.create.sql` against `localhost:9020` / `localhost:9010`.
  - Verified creation of 62 tables, 60 foreign keys, and 219 secondary/unique indexes.
  - Verified standalone create and drop cycles for all submodules with 0 residual objects.

---

## Conclusion
Unit 6 is thoroughly verified as correct, complete, and validated with high test integrity on Cloud Spanner GoogleSQL.

**Overall Status: ACCEPT**
