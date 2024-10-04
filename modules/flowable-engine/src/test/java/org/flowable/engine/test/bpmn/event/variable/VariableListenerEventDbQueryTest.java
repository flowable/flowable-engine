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

package org.flowable.engine.test.bpmn.event.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.profiler.CommandStats;
import org.flowable.engine.test.profiler.ConsoleLogger;
import org.flowable.engine.test.profiler.FlowableProfiler;
import org.flowable.engine.test.profiler.ProfileSession;
import org.flowable.engine.test.profiler.ProfilingDbSqlSessionFactory;
import org.flowable.engine.test.profiler.TotalExecutionTimeCommandInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
public class VariableListenerEventDbQueryTest extends PluggableFlowableTestCase {
    
    protected CommandInterceptor oldFirstCommandInterceptor;
    protected DbSqlSessionFactory oldDbSqlSessionFactory;
    protected HistoryLevel oldHistoryLevel;
    
    @BeforeEach
    protected void setUp() throws Exception {
        // The time interceptor should be first
        CommandExecutorImpl commandExecutor = ((CommandExecutorImpl) processEngineConfiguration.getCommandExecutor());
        this.oldFirstCommandInterceptor = commandExecutor.getFirst();
        oldHistoryLevel = processEngineConfiguration.getHistoryLevel();

        TotalExecutionTimeCommandInterceptor timeCommandInterceptor = new TotalExecutionTimeCommandInterceptor();
        timeCommandInterceptor.setNext(oldFirstCommandInterceptor);
        commandExecutor.setFirst(timeCommandInterceptor);
        
        // Add dbsqlSession factory that captures CRUD operations
        this.oldDbSqlSessionFactory = processEngineConfiguration.getDbSqlSessionFactory();
        DbSqlSessionFactory newDbSqlSessionFactory = new ProfilingDbSqlSessionFactory(processEngineConfiguration.isUsePrefixId());
        newDbSqlSessionFactory.setBulkInserteableEntityClasses(new HashSet<>(EntityDependencyOrder.INSERT_ORDER));
        newDbSqlSessionFactory.setInsertionOrder(oldDbSqlSessionFactory.getInsertionOrder());
        newDbSqlSessionFactory.setDeletionOrder(oldDbSqlSessionFactory.getDeletionOrder());
        newDbSqlSessionFactory.setDatabaseType(oldDbSqlSessionFactory.getDatabaseType());
        newDbSqlSessionFactory.setDatabaseTablePrefix(oldDbSqlSessionFactory.getDatabaseTablePrefix());
        newDbSqlSessionFactory.setTablePrefixIsSchema(oldDbSqlSessionFactory.isTablePrefixIsSchema());
        newDbSqlSessionFactory.setDatabaseCatalog(oldDbSqlSessionFactory.getDatabaseCatalog());
        newDbSqlSessionFactory.setDatabaseSchema(oldDbSqlSessionFactory.getDatabaseSchema());
        newDbSqlSessionFactory.setSqlSessionFactory(oldDbSqlSessionFactory.getSqlSessionFactory());
        newDbSqlSessionFactory.setDbHistoryUsed(oldDbSqlSessionFactory.isDbHistoryUsed());
        newDbSqlSessionFactory.setDatabaseSpecificStatements(oldDbSqlSessionFactory.getDatabaseSpecificStatements());
        newDbSqlSessionFactory.setLogicalNameToClassMapping(oldDbSqlSessionFactory.getLogicalNameToClassMapping());
        processEngineConfiguration.addSessionFactory(newDbSqlSessionFactory);
        
        processEngineConfiguration.setHistoryLevel(HistoryLevel.AUDIT);
    }
    
    @AfterEach
    protected void tearDown() throws Exception {
        ((CommandExecutorImpl) processEngineConfiguration.getCommandExecutor()).setFirst(oldFirstCommandInterceptor);
        processEngineConfiguration.addSessionFactory(oldDbSqlSessionFactory);
        processEngineConfiguration.setHistoryLevel(oldHistoryLevel);
    }

    @Test
    @Deployment
    public void catchVariableListenerDbQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchVariableListener");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        restartProfiling("Event subscription session");
        runtimeService.setVariable(processInstance.getId(), "nonExistingVar", "test");
        stopProfiling();
        
        assertDatabaseSelects("SetExecutionVariablesCmd",
                "selectById org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl", 1L,
                "selectVariablesByQuery", 1L);
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        restartProfiling("Event subscription session");
        runtimeService.setVariable(processInstance.getId(), "var1", "test");
        stopProfiling();
        
        assertDatabaseSelects("SetExecutionVariablesCmd",
                "selectById org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl", 2L,
                "selectById org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl", 1L,
                "selectById org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl", 1L,
                "selectChildExecutionsByProcessInstanceId", 1L,
                "selectDeadLetterJobsByExecutionId", 1L,
                "selectJobsByExecutionId", 1L,
                "selectSuspendedJobsByExecutionId", 1L,
                "selectTimerJobsByExecutionId", 1L,
                "selectExternalWorkerJobsByExecutionId", 1L,
                "selectVariablesByQuery", 3L,
                "selectEntityLinksByQuery", 1L,
                "selectExecutionsByParentExecutionId", 3L,
                "selectIdentityLinksByProcessInstance", 1L,
                "selectProcessDefinitionInfoByProcessDefinitionId", 1L,
                "selectSubProcessInstanceBySuperExecutionId", 1L,
                "selectTasksByExecutionId", 1L,
                "selectUnfinishedActivityInstanceExecutionIdAndActivityId", 2L,
                "selectEventSubscriptionsByExecution", 2L,
                "selectEventSubscriptionsByProcessInstanceAndType", 2L);
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
    }
    
    protected void assertDatabaseSelects(String commandClass, Object... expectedSelects) {
        CommandStats stats = getStats(commandClass);

        assertThat(stats.getDbSelects())
                .as("Unexpected number of database selects for " + commandClass + ". ")
                .hasSize(expectedSelects.length / 2);

        for (int i = 0; i < expectedSelects.length; i += 2) {
            String dbSelect = (String) expectedSelects[i];
            Long count = (Long) expectedSelects[i + 1];

            assertThat(stats.getDbSelects())
                    .as("Wrong select count for " + dbSelect)
                    .containsEntry(dbSelect, count);
        }
    }
    
    protected CommandStats getStats(String commandClass) {
        ProfileSession profileSession = FlowableProfiler.getInstance().getProfileSessions().get(0);
        Map<String, CommandStats> allStats = profileSession.calculateSummaryStatistics();
        CommandStats stats = getStatsForCommand(commandClass, allStats);
        return stats;
    }

    protected CommandStats getStatsForCommand(String commandClass, Map<String, CommandStats> allStats) {
        String clazz = commandClass;
        if (!clazz.startsWith("org.flowable")) {
            clazz = "org.flowable.engine.impl.cmd." + clazz;
        }
        CommandStats stats = allStats.get(clazz);
        return stats;
    }
    
    protected FlowableProfiler restartProfiling(String sessionName) {
        FlowableProfiler flowableProfiler = FlowableProfiler.getInstance();
        flowableProfiler.reset();
        flowableProfiler.startProfileSession(sessionName);
        return flowableProfiler;
    }

    protected void stopProfiling() {
        FlowableProfiler profiler = FlowableProfiler.getInstance();
        profiler.stopCurrentProfileSession();
        new ConsoleLogger(profiler).log();
    }
}
