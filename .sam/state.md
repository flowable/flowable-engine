# SAM Migration State

- **Branch:** `spanner-migration`
- **Stage:** Conversion (Unit 1 Complete, moving to Unit 2)
- **Status:** In Progress
- **Updated:** 2026-09-25T06:56:45Z

## Confirmed Stack & Policies

| Dimension | Policy / Choice | Status |
| :--- | :--- | :--- |
| **Language** | Java 17 | Confirmed |
| **Build Tool** | Apache Maven 3.9.12 | Confirmed |
| **Data Access / ORM** | MyBatis 3.5.19 | Confirmed (Override) |
| **Target Database** | Cloud Spanner | Confirmed |
| **Dialect** | GoogleSQL | Confirmed |
| **Isolation Level** | SERIALIZABLE | Confirmed |
| **Validation Target** | Cloud Spanner Emulator | Confirmed |
| **Table Hierarchy** | Flat standalone tables with secondary indexes | Confirmed |
| **Primary Key Policy** | StrongUuidGenerator (UUID) | Confirmed |
| **Entity ID Type** | STRING(64) | Confirmed |
| **DDL Execution** | Batch DDL (`START BATCH DDL ... RUN BATCH`) | Confirmed |
| **Transaction Abort** | Automatic retries via SpannerRetryInterceptor | Confirmed |

## Pipeline Progress

- [x] Scout: Stack Discovery (Phase 1)
- [x] Scout: Codebase Inventory Scan (Phase 2)
- [x] Planner: Migration Plan & Dependency Graph (Approved)
- [ ] Conversion & Review:
  - [x] Unit 1: Root Spanner Configuration (Accepted & Committed)
  - [ ] Unit 2: Spanner Engine Dialect & Session Configuration
  - [ ] Unit 3: Transaction Abort Handling & Spanner Retry Interceptor
  - [ ] Unit 4: MyBatis Mapper Adjustments for GoogleSQL
  - [ ] Unit 5: Engine Common & Process Engine Schema DDL Scripts
  - [ ] Unit 6: Sub-Engines Schema DDL Scripts
  - [ ] Unit 7: Verification & Test Harness for Cloud Spanner
