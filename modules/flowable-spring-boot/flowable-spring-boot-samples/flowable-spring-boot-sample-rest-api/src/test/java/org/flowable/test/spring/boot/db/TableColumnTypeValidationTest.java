/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.test.spring.boot.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import flowable.Application;

/**
 * This test was introduced after the major refactoring of the varchar / nvarchar mappings of colum types in the MyBatis files.
 * The @{@link EngineMappingsValidationTest} tests the mapping xml's itself, this test compares the expected column types with the real JDBC metadata.
 * Since it runs on the QA env, it will be validated for each database.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TableColumnTypeValidationTest {

    @Autowired
    protected ProcessEngine processEngine; // The actual engine used is not relevant, any engine would give the same result, but the process engine mgmtService can execute commands

    protected String databaseType;

    @BeforeEach
    public void setUp() {
        this.databaseType = processEngine.getProcessEngineConfiguration().getDatabaseType();
    }

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityHelperUtil.EntityPackageTestArgumentsProvider.class)
    public void validateColumnTypes(EntityHelperUtil.EntityMappingPackageInformation packageInformation) throws IOException {

        Map<String, String> mappedResources = EntityHelperUtil.readMappingFileAsString(packageInformation);
        assertThat(mappedResources).isNotEmpty();

        for (String entity : mappedResources.keySet()) {
            String tableName = findTable(entity, mappedResources.get(entity));

            Map<String, String> columnNameToTypeMap = getColumnMetaData(tableName);
            assertThat(columnNameToTypeMap)
                    .withFailMessage("No column metadata found for <" + tableName + ">")
                    .isNotEmpty();

            for (String columnName : columnNameToTypeMap.keySet()) {
                String columnTypeFromMetaData = columnNameToTypeMap.get(columnName);
                String configuredColumnType = EntityParameterTypesOverview.getColumnType(entity, columnName);

                // All mapping files are configured to be nvarchar. However, only SQL server actually uses it.
                // For all the other database types, the custom type handler changes it to varchar when needed.
                if (!columnTypeFromMetaData.equals(EntityParameterTypesOverview.PARAMETER_TYPE_NVARCHAR)
                        && configuredColumnType.equalsIgnoreCase(EntityParameterTypesOverview.PARAMETER_TYPE_NVARCHAR) &&
                        !databaseType.equals(AbstractEngineConfiguration.DATABASE_TYPE_MSSQL)) {
                    configuredColumnType = EntityParameterTypesOverview.PARAMETER_TYPE_VARCHAR;
                }

                // Some databases report boolean for tinyint columns
                if (columnTypeFromMetaData.equalsIgnoreCase(EntityParameterTypesOverview.PARAMETER_TYPE_BOOLEAN)) {
                    assertThat(configuredColumnType)
                            .withFailMessage("Column type does not match. Expecting <" + configuredColumnType
                                    + "> for column " + columnName + " of table " + tableName + ", but JDBC metadata returned <" + columnTypeFromMetaData + ">")
                            .satisfiesAnyOf(
                                    param -> assertThat(param).isEqualTo(EntityParameterTypesOverview.PARAMETER_TYPE_BOOLEAN),
                                    param -> assertThat(param).isEqualTo(EntityParameterTypesOverview.PARAMETER_TYPE_INTEGER)
                            );

                } else {

                    assertThat(configuredColumnType)
                            .withFailMessage("Column type does not match. Expecting <" + configuredColumnType
                                    + "> for column " + columnName + " of table " + tableName + ", but JDBC metadata returned <" + columnTypeFromMetaData + ">")
                            .isEqualToIgnoringCase(columnTypeFromMetaData);

                }

            }

        }

    }

    private static String FROM_PATTERN = "from ${prefix}";

    private String findTable(String entity, String mappingFileContent) {

        // CaseInstance is an exception: there's more ACT_RU_ENTITYLINK in there than case instance
        if (entity.equalsIgnoreCase("CaseInstance")) {
            return "ACT_CMMN_RU_CASE_INST";
        } else if (entity.equalsIgnoreCase("HistoricCaseInstance")) {
            return "ACT_CMMN_HI_CASE_INST";
        } else if (entity.equalsIgnoreCase("Privilege")) {
            return "ACT_ID_PRIV";
        }

        // Simplistic approach: check for ${prefix}<TABLE> patterns. The one that is most in the mapping file, is most likely the table name.
        int currentIndex = 0;
        Map<String, AtomicInteger> tables = new HashMap<>();
        do {
            currentIndex = mappingFileContent.indexOf(FROM_PATTERN, currentIndex);

            if (currentIndex >= 0) {

                currentIndex += FROM_PATTERN.length();

                int spaceIndex = mappingFileContent.indexOf(" ", currentIndex);
                String tableName = mappingFileContent.substring(currentIndex, spaceIndex);

                if (!tables.containsKey(tableName)) {
                    tables.put(tableName, new AtomicInteger(0));
                }
                tables.get(tableName).incrementAndGet();
            }

        } while (currentIndex >= 0);

        String result = null;
        for (String tableName : tables.keySet()) {
            int count = tables.get(tableName).get();
            if (result == null || count > tables.get(result).get()) {
                result = tableName;
            }
        }

        return result.trim();
    }

    protected Map<String, String> getColumnMetaData(String tableName) {
        return processEngine.getManagementService().executeCommand(commandContext -> {
            try {
                DbSqlSession dbSqlSession = commandContext.getSession(DbSqlSession.class);
                Connection connection = dbSqlSession.getSqlSession().getConnection();
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                String prefixedTableName = tableName;
                String databaseTablePrefix = dbSqlSession.getDbSqlSessionFactory().getDatabaseTablePrefix();
                if (StringUtils.isNotEmpty(databaseTablePrefix)) {
                    prefixedTableName = databaseTablePrefix + "." + tableName;
                }
                String catalog = processEngine.getProcessEngineConfiguration().getDatabaseCatalog();
                String schema = processEngine.getProcessEngineConfiguration().getDatabaseSchema();
                if (StringUtils.isNotEmpty(schema) && AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equalsIgnoreCase(databaseType)) {
                    schema = schema.toUpperCase(Locale.ROOT);
                }

                Map<String, String> columnNameToTypeMap = new HashMap<>();

                if (databaseType.equals(AbstractEngineConfiguration.DATABASE_TYPE_MSSQL) && StringUtils.isEmpty(catalog)) {
                    catalog = null; // Otherwise SQL server errors out
                }

                ResultSet resultSet = databaseMetaData.getColumns(catalog, schema, prefixedTableName, null);
                while (resultSet.next()) {
                    String columnName = resultSet.getString("COLUMN_NAME");
                    String columnType = resultSet.getString("TYPE_NAME");

                    // The JDBC metadata API doesn't return the same names as used in the mybatis mapping files
                    if (columnType.equalsIgnoreCase("BINARY LARGE OBJECT")
                            || columnType.equalsIgnoreCase("varbinary")
                            || columnType.equalsIgnoreCase("BINARY VARYING")
                            || columnType.equalsIgnoreCase("LONGBLOB") // mariadb
                            || columnType.equalsIgnoreCase("bytea")) { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_BLOBTYPE;

                    } else if (columnType.equalsIgnoreCase("CHARACTER VARYING")
                            || columnType.equalsIgnoreCase("CHARACTER LARGE OBJECT")
                            || columnType.equalsIgnoreCase("VARCHAR2") // oracle
                            || columnType.equalsIgnoreCase("LONGTEXT") // mariadb
                            || columnType.equalsIgnoreCase("text")) { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_VARCHAR;

                    } else if (columnType.equalsIgnoreCase("DOUBLE PRECISION")
                            || columnType.equalsIgnoreCase("float")
                            || columnType.equalsIgnoreCase("float8")) { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_DOUBLE;

                    } else if (columnType.equalsIgnoreCase("datetime")
                            || columnType.equalsIgnoreCase("datetime2")) {
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_TIMESTAMP;

                    } else if (columnType.equalsIgnoreCase("int")
                            || columnType.equalsIgnoreCase("int4")) { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_INTEGER;

                    } else if (columnType.equalsIgnoreCase("int8"))  { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_BIGINT;

                    } else if (columnType.equalsIgnoreCase("bit")
                            || columnType.equalsIgnoreCase("bool")) { // postgres
                        columnType = EntityParameterTypesOverview.PARAMETER_TYPE_BOOLEAN;

                    }

                    columnNameToTypeMap.put(columnName, columnType);
                }

                return columnNameToTypeMap;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
