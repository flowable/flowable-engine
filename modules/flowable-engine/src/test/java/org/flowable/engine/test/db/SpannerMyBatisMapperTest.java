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
package org.flowable.engine.test.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests MyBatis mapper SQL generation for Cloud Spanner (GoogleSQL) vs other database dialects.
 */
public class SpannerMyBatisMapperTest {

    private static final Pattern DELETE_ALIAS_FROM_PATTERN = Pattern.compile("(?s)(?i)^\\s*delete\\s+(?!from\\b)[a-z0-9_]+\\s+from.*");

    private static Configuration spannerConfiguration;
    private static Configuration mysqlConfiguration;

    @BeforeAll
    public static void setUpConfigurations() throws Exception {
        spannerConfiguration = buildConfiguration(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
        mysqlConfiguration = buildConfiguration("mysql");
    }

    private static class TestConfiguration extends StandaloneInMemProcessEngineConfiguration {
        TestConfiguration(String databaseType) {
            this.databaseType = databaseType;
            this.databaseSchemaUpdate = DB_SCHEMA_UPDATE_FALSE;
            this.jdbcUrl = "jdbc:h2:mem:flowable-mapper-" + databaseType;
        }

        public Configuration initAndGetMyBatisConfiguration() {
            initVariableTypes();
            initDataSource();
            initTransactionFactory();
            initSqlSessionFactory();
            return getSqlSessionFactory().getConfiguration();
        }
    }

    private static Configuration buildConfiguration(String databaseType) {
        return new TestConfiguration(databaseType).initAndGetMyBatisConfiguration();
    }

    private String getGeneratedSql(Configuration configuration, String statementId, Object parameter) {
        MappedStatement ms = configuration.getMappedStatement(statementId);
        assertThat(ms).as("MappedStatement '%s' should exist", statementId).isNotNull();
        BoundSql boundSql = ms.getBoundSql(parameter);
        return boundSql.getSql();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bulkDeleteHistoricDetailForNonExistingProcessInstances",
            "bulkDeleteHistoricActivityInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricVariableInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricVariableInstancesForNonExistingCaseInstances",
            "bulkDeleteHistoricTaskLogEntriesForNonExistingProcessInstances",
            "bulkDeleteHistoricTaskLogEntriesForNonExistingCaseInstances",
            "bulkDeleteHistoricTaskInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricTaskInstancesForNonExistingCaseInstances",
            "bulkDeleteHistoricProcessIdentityLinks",
            "bulkDeleteHistoricCaseIdentityLinks",
            "bulkDeleteHistoricTaskIdentityLinks"
    })
    public void testDeleteStatementsDoNotIncludeAliasForSpanner(String statementId) {
        String spannerSql = getGeneratedSql(spannerConfiguration, statementId, Collections.emptyMap());
        assertThat(spannerSql)
                .as("Spanner SQL for statement '%s' should start with 'delete from' and not contain an alias between delete and from", statementId)
                .doesNotMatch(DELETE_ALIAS_FROM_PATTERN);

        String normalized = spannerSql.replaceAll("\\s+", " ").trim().toLowerCase();
        assertThat(normalized)
                .as("Spanner SQL for statement '%s' must begin with 'delete from'", statementId)
                .startsWith("delete from");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bulkDeleteHistoricDetailForNonExistingProcessInstances",
            "bulkDeleteHistoricActivityInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricVariableInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricVariableInstancesForNonExistingCaseInstances",
            "bulkDeleteHistoricTaskLogEntriesForNonExistingProcessInstances",
            "bulkDeleteHistoricTaskLogEntriesForNonExistingCaseInstances",
            "bulkDeleteHistoricTaskInstancesForNonExistingProcessInstances",
            "bulkDeleteHistoricTaskInstancesForNonExistingCaseInstances",
            "bulkDeleteHistoricProcessIdentityLinks",
            "bulkDeleteHistoricCaseIdentityLinks",
            "bulkDeleteHistoricTaskIdentityLinks"
    })
    public void testDeleteStatementsPreserveAliasForMySql(String statementId) {
        String mysqlSql = getGeneratedSql(mysqlConfiguration, statementId, Collections.emptyMap());
        assertThat(mysqlSql)
                .as("MySQL SQL for statement '%s' should retain legacy delete alias syntax", statementId)
                .matches(DELETE_ALIAS_FROM_PATTERN);
    }

    @Test
    public void testChangeTenantBpmnStatementsForSpanner() {
        Map<String, Object> params = new HashMap<>();
        params.put("targetTenantId", "tenant2");
        params.put("sourceTenantId", "tenant1");

        String sql = getGeneratedSql(spannerConfiguration, "changeTenantIdExecutions", params);
        String normalized = sql.replaceAll("\\s+", " ").trim().toLowerCase();
        assertThat(normalized)
                .as("Spanner changeTenantIdExecutions should use GoogleSQL compatible update syntax")
                .startsWith("update act_ru_execution res");
        assertThat(sql)
                .as("Spanner changeTenantIdExecutions must include WHERE clause")
                .containsIgnoringCase("where")
                .as("Spanner changeTenantIdExecutions should not contain mssql FROM clause")
                .doesNotContain("FROM ACT_RU_EXECUTION RES");
    }
}
