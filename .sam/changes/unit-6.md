# Unit 6: Sub-Engines Schema DDL Scripts - Change Summary

## Overview
Authored Cloud Spanner (GoogleSQL dialect) schema DDL creation and teardown scripts for all Flowable sub-engines:
- IDM Engine (`flowable-idm-engine`)
- CMMN Engine (`flowable-cmmn-engine`)
- DMN Engine (`flowable-dmn-engine`)
- App Engine (`flowable-app-engine`)
- Event Registry Engine (`flowable-event-registry`)
- Unified distribution script (`distro/sql/create/all/flowable.spanner.all.create.sql`)

All scripts strictly comply with Cloud Spanner GoogleSQL constraints: native GoogleSQL types, mandatory table-level `PRIMARY KEY` specifications, `CREATE UNIQUE NULL_FILTERED INDEX` for nullable uniqueness constraints, strict drop ordering (Foreign Keys -> Secondary/Unique Indexes -> Tables in reverse dependency order), absence of unsupported `IF EXISTS` on `DROP CONSTRAINT`, and 1:1 symmetry between creation and teardown scripts.

## Target Files
### Production DDL Scripts
1. **IDM Engine**:
   - [`modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/create/flowable.spanner.create.identity.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/create/flowable.spanner.create.identity.sql)
   - [`modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/drop/flowable.spanner.drop.identity.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/main/resources/org/flowable/idm/db/drop/flowable.spanner.drop.identity.sql)
2. **CMMN Engine**:
   - [`modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/create/flowable.spanner.create.cmmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/create/flowable.spanner.create.cmmn.sql)
   - [`modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/drop/flowable.spanner.drop.cmmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/main/resources/org/flowable/cmmn/db/drop/flowable.spanner.drop.cmmn.sql)
3. **DMN Engine**:
   - [`modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/create/flowable.spanner.create.dmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/create/flowable.spanner.create.dmn.sql)
   - [`modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/drop/flowable.spanner.drop.dmn.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/main/resources/org/flowable/dmn/db/drop/flowable.spanner.drop.dmn.sql)
4. **App Engine**:
   - [`modules/flowable-app-engine/src/main/resources/org/flowable/app/db/create/flowable.spanner.create.app.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/main/resources/org/flowable/app/db/create/flowable.spanner.create.app.sql)
   - [`modules/flowable-app-engine/src/main/resources/org/flowable/app/db/drop/flowable.spanner.drop.app.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/main/resources/org/flowable/app/db/drop/flowable.spanner.drop.app.sql)
5. **Event Registry**:
   - [`modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/create/flowable.spanner.create.eventregistry.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/create/flowable.spanner.create.eventregistry.sql)
   - [`modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/drop/flowable.spanner.drop.eventregistry.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/main/resources/org/flowable/eventregistry/db/drop/flowable.spanner.drop.eventregistry.sql)
6. **Distro Unified Script**:
   - [`distro/sql/create/all/flowable.spanner.all.create.sql`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/distro/sql/create/all/flowable.spanner.all.create.sql)

### Unit Tests
- [`modules/flowable-idm-engine/src/test/java/org/flowable/idm/engine/impl/db/SpannerIdmDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-idm-engine/src/test/java/org/flowable/idm/engine/impl/db/SpannerIdmDdlTest.java)
- [`modules/flowable-cmmn-engine/src/test/java/org/flowable/cmmn/engine/impl/db/SpannerCmmnDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-cmmn-engine/src/test/java/org/flowable/cmmn/engine/impl/db/SpannerCmmnDdlTest.java)
- [`modules/flowable-dmn-engine/src/test/java/org/flowable/dmn/engine/impl/db/SpannerDmnDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dmn-engine/src/test/java/org/flowable/dmn/engine/impl/db/SpannerDmnDdlTest.java)
- [`modules/flowable-app-engine/src/test/java/org/flowable/app/engine/impl/db/SpannerAppDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-app-engine/src/test/java/org/flowable/app/engine/impl/db/SpannerAppDdlTest.java)
- [`modules/flowable-event-registry/src/test/java/org/flowable/eventregistry/impl/db/SpannerEventRegistryDdlTest.java`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-event-registry/src/test/java/org/flowable/eventregistry/impl/db/SpannerEventRegistryDdlTest.java)

## Details of Changes

### 1. IDM Engine
- **Create DDL (`flowable.spanner.create.identity.sql`)**:
  - Tables: `ACT_ID_PROPERTY`, `ACT_ID_BYTEARRAY`, `ACT_ID_GROUP`, `ACT_ID_MEMBERSHIP`, `ACT_ID_USER`, `ACT_ID_INFO`, `ACT_ID_TOKEN`, `ACT_ID_PRIV`, `ACT_ID_PRIV_MAPPING`.
  - Primary Keys: Table-level `PRIMARY KEY (...)` on each table.
  - Foreign Keys: `ACT_FK_MEMB_GROUP`, `ACT_FK_MEMB_USER`, `ACT_FK_PRIV_MAPPING`.
  - Indexes: `ACT_IDX_MEMB_GROUP`, `ACT_IDX_MEMB_USER`, `ACT_IDX_PRIV_MAPPING`, `ACT_IDX_PRIV_USER`, `ACT_IDX_PRIV_GROUP`.
  - Nullable Uniqueness: `CREATE UNIQUE NULL_FILTERED INDEX ACT_UNIQ_PRIV_NAME ON ACT_ID_PRIV(NAME_);`.
  - Seed Property: `INSERT INTO ACT_ID_PROPERTY (NAME_, VALUE_, REV_) VALUES ('schema.version', '8.1.0.1', 1);`.
- **Drop DDL (`flowable.spanner.drop.identity.sql`)**:
  - Drops 3 FK constraints (`ALTER TABLE <table> DROP CONSTRAINT <constraint>;`).
  - Drops 6 indexes (`DROP INDEX IF EXISTS <index>;`).
  - Drops 9 tables in reverse dependency order.

### 2. CMMN Engine
- **Create DDL (`flowable.spanner.create.cmmn.sql`)**:
  - Tables: `ACT_CMMN_DEPLOYMENT`, `ACT_CMMN_DEPLOYMENT_RESOURCE`, `ACT_CMMN_CASEDEF`, `ACT_CMMN_RU_CASE_INST`, `ACT_CMMN_RU_PLAN_ITEM_INST`, `ACT_CMMN_RU_SENTRY_PART_INST`, `ACT_CMMN_RU_MIL_INST`, `ACT_CMMN_HI_CASE_INST`, `ACT_CMMN_HI_MIL_INST`, `ACT_CMMN_HI_PLAN_ITEM_INST`.
  - Foreign Keys: `ACT_FK_CMMN_RSRC_DPL`, `ACT_FK_CASE_DEF_DPLY`, `ACT_FK_CASE_INST_CASE_DEF`, `ACT_FK_PLAN_ITEM_CASE_DEF`, `ACT_FK_PLAN_ITEM_CASE_INST`, `ACT_FK_SENTRY_CASE_DEF`, `ACT_FK_SENTRY_CASE_INST`, `ACT_FK_SENTRY_PLAN_ITEM`, `ACT_FK_MIL_CASE_DEF`, `ACT_FK_MIL_CASE_INST`.
  - Indexes: 15 secondary indexes + 1 unique null-filtered index (`ACT_IDX_CASE_DEF_UNIQ`).
  - Seed Property: `INSERT INTO ACT_GE_PROPERTY (NAME_, VALUE_, REV_) VALUES ('cmmn.schema.version', '8.1.0.1', 1);`.
- **Drop DDL (`flowable.spanner.drop.cmmn.sql`)**:
  - Drops 10 FK constraints without `IF EXISTS`.
  - Drops 16 secondary/unique indexes with `DROP INDEX IF EXISTS`.
  - Drops 10 CMMN tables in reverse dependency order.

### 3. DMN Engine
- **Create DDL (`flowable.spanner.create.dmn.sql`)**:
  - Tables: `ACT_DMN_DEPLOYMENT`, `ACT_DMN_DEPLOYMENT_RESOURCE`, `ACT_DMN_DECISION`, `ACT_DMN_HI_DECISION_EXECUTION`.
  - Foreign Keys: `ACT_FK_DMN_RSRC_DPL`.
  - Indexes: `ACT_IDX_DMN_RSRC_DPL`, `ACT_IDX_DMN_INSTANCE_ID`, and unique null-filtered `ACT_IDX_DMN_DEC_UNIQ`.
  - Seed Property: `INSERT INTO ACT_GE_PROPERTY (NAME_, VALUE_, REV_) VALUES ('dmn.schema.version', '8.1.0.1', 1);`.
- **Drop DDL (`flowable.spanner.drop.dmn.sql`)**:
  - Drops 1 FK constraint (`ALTER TABLE ACT_DMN_DEPLOYMENT_RESOURCE DROP CONSTRAINT ACT_FK_DMN_RSRC_DPL;`).
  - Drops 3 indexes with `DROP INDEX IF EXISTS`.
  - Drops 4 DMN tables in reverse dependency order.

### 4. App Engine
- **Create DDL (`flowable.spanner.create.app.sql`)**:
  - Tables: `ACT_APP_DEPLOYMENT`, `ACT_APP_DEPLOYMENT_RESOURCE`, `ACT_APP_APPDEF`.
  - Foreign Keys: `ACT_FK_APP_RSRC_DPL`, `ACT_FK_APP_DEF_DPLY`.
  - Indexes: `ACT_IDX_APP_RSRC_DPL`, `ACT_IDX_APP_DEF_DPLY`, and unique null-filtered `ACT_IDX_APP_DEF_UNIQ`.
  - Seed Property: `INSERT INTO ACT_GE_PROPERTY (NAME_, VALUE_, REV_) VALUES ('app.schema.version', '8.1.0.1', 1);`.
- **Drop DDL (`flowable.spanner.drop.app.sql`)**:
  - Drops 2 FK constraints (`ACT_FK_APP_RSRC_DPL`, `ACT_FK_APP_DEF_DPLY`).
  - Drops 3 indexes with `DROP INDEX IF EXISTS`.
  - Drops 3 App tables in reverse dependency order.

### 5. Event Registry
- **Create DDL (`flowable.spanner.create.eventregistry.sql`)**:
  - Tables: `FLW_EVENT_DEPLOYMENT`, `FLW_EVENT_RESOURCE`, `FLW_EVENT_DEFINITION`, `FLW_CHANNEL_DEFINITION`.
  - Foreign Keys: `FLW_FK_EVENT_RSRC_DPL`.
  - Indexes: `FLW_IDX_EVENT_RSRC_DPL`, plus unique null-filtered `ACT_IDX_EVENT_DEF_UNIQ` and `ACT_IDX_CHANNEL_DEF_UNIQ`.
  - Seed Property: `INSERT INTO ACT_GE_PROPERTY (NAME_, VALUE_, REV_) VALUES ('eventregistry.schema.version', '8.1.0.1', 1);`.
- **Drop DDL (`flowable.spanner.drop.eventregistry.sql`)**:
  - Drops 1 FK constraint (`ALTER TABLE FLW_EVENT_RESOURCE DROP CONSTRAINT FLW_FK_EVENT_RSRC_DPL;`).
  - Drops 3 indexes with `DROP INDEX IF EXISTS`.
  - Drops 4 Event Registry tables in reverse dependency order.

### 6. Distro Unified Script (`flowable.spanner.all.create.sql`)
- Concatenates the schema create DDL for all sub-engines in dependency order:
  `Common -> Engine Core -> History -> App Engine -> CMMN Engine -> DMN Engine -> Event Registry -> IDM Engine`.
- Yields a standalone, 1575-line distribution script capable of initializing the entire Flowable persistence layer in a single execution.

## Decisions & Defaults
- **Flat Standalone Tables ([`SPN-D4`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/.sam/plan.md#L46))**: All sub-engine tables use standalone primary keys and standard foreign keys rather than interleaving, preserving Flowable's uniform key conventions and independent lifecycle management.
- **Nullable Uniqueness ([`GSQL-C5`](file:///usr/local/google/home/vaibhavch/.gemini/config/plugins/sam/skills/sam-knowledge/references/spanner/dialect/googlesql/conversion.md#L68))**: Defined all business-key/version/tenant unique indexes with `CREATE UNIQUE NULL_FILTERED INDEX` across definition and metadata tables to prevent duplicate collision errors on null column values.
- **Drop Syntax & Ordering**:
  - Cloud Spanner GoogleSQL does not support `IF EXISTS` on `ALTER TABLE <table> DROP CONSTRAINT <constraint>;`.
  - Foreign keys and indexes must be explicitly dropped before tables.
  - Teardown order strictly enforced: Drop FKs -> Drop Indexes -> Drop Tables in reverse dependency order.

## Verification & Test Status
- Test-Driven Development (TDD):
  - **Red Phase**: Unit tests authored first asserting classpath availability, syntax compliance, lack of unsupported clauses, and symmetric drop statements. Confirmed failure across modules due to missing resources.
  - **Green Phase**: Authored DDL scripts and re-executed tests, achieving 100% pass rate.
- Test Execution:
  `./mvnw test -pl modules/flowable-idm-engine,modules/flowable-cmmn-engine,modules/flowable-dmn-engine,modules/flowable-app-engine,modules/flowable-event-registry -am -Dtest=*Spanner*,*Ddl* -Dsurefire.failIfNoSpecifiedTests=false`
  - Tests run: 17, Failures: 0, Errors: 0, Skipped: 0.
  - Build status: `SUCCESS`.
- Checkstyle Audit:
  `./mvnw checkstyle:check -pl modules/flowable-idm-engine,modules/flowable-cmmn-engine,modules/flowable-dmn-engine,modules/flowable-app-engine,modules/flowable-event-registry`
  - 0 Checkstyle violations across all 5 submodules.
