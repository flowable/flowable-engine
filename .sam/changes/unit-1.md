# Unit 1: Root Spanner Configuration - Change Summary

## Overview
Introduced the Cloud Spanner JDBC driver dependency management and build profile to Flowable Engine without modifying default build behavior or existing database drivers.

## Modified Files
- [`modules/flowable-dependencies/pom.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-dependencies/pom.xml)
- [`modules/flowable-parent/pom.xml`](file:///usr/local/google/home/vaibhavch/sam-teamfood/flowable-engine/modules/flowable-parent/pom.xml)

## Details of Changes
1. **`modules/flowable-dependencies/pom.xml`**:
   - Added `<google-cloud-spanner-jdbc.version>2.27.0</google-cloud-spanner-jdbc.version>` under `<!-- JDBC Driver dependencies -->`.
   - Added `com.google.cloud:google-cloud-spanner-jdbc` to the `<dependencyManagement><dependencies>` section under `<!-- JDBC Drivers -->`.
2. **`modules/flowable-parent/pom.xml`**:
   - Added `<profile><id>spanner</id>` declaring the dependency `com.google.cloud:google-cloud-spanner-jdbc` alongside existing database profiles (`postgresql`, `mssql`, `mysql`, `mariadb`, `db2`, `oracle`).
3. **Compatibility Preserved**:
   - Existing database driver dependencies, versions, and configurations were left untouched.
   - The default Maven build profile remains standard H2 in-memory.

## Decisions & Defaults
- **Driver Version**: Used confirmed version `2.27.0` for `com.google.cloud:google-cloud-spanner-jdbc` per plan specification.
- **Dependency Management Location**: Defined in `flowable-dependencies` alongside other JDBC drivers (`db2-jcc`, `h2`, `mssql-jdbc`, `mysql-connector-j`, `ojdbc17`, `postgresql`) ensuring uniform version management across all Flowable engine modules.
- **Profile Configuration**: Followed existing convention in `flowable-parent` where database profiles include only their corresponding driver artifact without version (inheriting from `dependencyManagement`).

## Verification & Test Status
- Ran default Maven build:
  `./mvnw test-compile -pl modules/flowable-dependencies,modules/flowable-parent -DskipTests` -> **BUILD SUCCESS**
- Ran Spanner profile dependency resolution:
  `./mvnw dependency:resolve -pl modules/flowable-parent -Pspanner` -> **BUILD SUCCESS** (successfully resolved `google-cloud-spanner-jdbc:2.27.0` and transitive Google Cloud/gRPC/protobuf libraries)
- Ran Spanner profile compile:
  `./mvnw test-compile -pl modules/flowable-dependencies,modules/flowable-parent -Pspanner -DskipTests` -> **BUILD SUCCESS**
