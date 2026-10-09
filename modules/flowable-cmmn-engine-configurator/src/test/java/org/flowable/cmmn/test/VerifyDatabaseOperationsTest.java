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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.test.profiler.FlowableProfiler;
import org.flowable.engine.test.profiler.ProfilingDbSqlSessionFactory;
import org.flowable.engine.test.profiler.TotalExecutionTimeCommandInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * @author Filip Hrisafov
 */
@DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
public class VerifyDatabaseOperationsTest extends AbstractProcessEngineIntegrationTest {

    protected DbSqlSessionFactory oldDbSqlSessionFactory;

    @BeforeEach
    protected void setUp() {
        // The process and the CMMN engine share the session factories
        ProcessEngineConfigurationImpl processEngineConfigurationImpl = (ProcessEngineConfigurationImpl) processEngineConfiguration;
        this.oldDbSqlSessionFactory = processEngineConfigurationImpl.getDbSqlSessionFactory();
        DbSqlSessionFactory newDbSqlSessionFactory = new ProfilingDbSqlSessionFactory(processEngineConfigurationImpl.isUsePrefixId());
        newDbSqlSessionFactory.setBulkInserteableEntityClasses(oldDbSqlSessionFactory.getBulkInserteableEntityClasses());
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
        processEngineConfigurationImpl.addSessionFactory(newDbSqlSessionFactory);
    }

    @AfterEach
    protected void tearDown() {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).addSessionFactory(oldDbSqlSessionFactory);
        FlowableProfiler.getInstance().reset();
    }

    @Test
    public void testCaseWithProcessTaskFromSameDeployment() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/processTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        Map<String, Long> selects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(), "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneProcessTaskCase")
                        .start());

        // The cached case definition has the parent deployment id, so the CMMN deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectProcessDefinitionByParentDeploymentAndKey", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L),
                entry("selectEntityLinksByQuery", 2L)
        );
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isOne();
    }

    @Test
    public void testCaseWithCaseTaskFromSameDeployment() {
        cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskSameDeploymentTrue.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        Map<String, Long> selects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(), "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneCaseTaskCase")
                        .start());

        // The cached case definition has the parent deployment id, so the CMMN deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 2L),
                entry("selectEntityLinksByQuery", 2L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").count()).isOne();
    }

    @Test
    public void testProcessWithCaseTaskFromSameDeployment() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithSameDeployment.bpmn20.xml")
                .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
                .deploy();

        Map<String, Long> selects = profileDatabaseSelects(processEngineConfiguration.getCommandExecutor(), "org.flowable.engine.impl.cmd.StartProcessInstanceCmd",
                () -> processEngineRuntimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("caseTask")
                        .start());

        // The cached process definition has the parent deployment id, so the process deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectLatestProcessDefinitionByKey", 1L),
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L),
                entry("selectEntityLinksByQuery", 2L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isOne();
    }

    protected Map<String, Long> profileDatabaseSelects(CommandExecutor commandExecutor, String commandClass, Runnable runnable) {
        // Only the outer command is profiled, the commands of the other engine reuse its command context
        CommandExecutorImpl commandExecutorImpl = (CommandExecutorImpl) commandExecutor;
        CommandInterceptor oldFirstCommandInterceptor = commandExecutorImpl.getFirst();
        TotalExecutionTimeCommandInterceptor timeCommandInterceptor = new TotalExecutionTimeCommandInterceptor();
        timeCommandInterceptor.setNext(oldFirstCommandInterceptor);
        commandExecutorImpl.setFirst(timeCommandInterceptor);

        FlowableProfiler profiler = FlowableProfiler.getInstance();
        profiler.reset();
        profiler.startProfileSession("Profiling session");
        try {
            runnable.run();
        } finally {
            profiler.stopCurrentProfileSession();
            commandExecutorImpl.setFirst(oldFirstCommandInterceptor);
        }

        return profiler.getProfileSessions().get(0).calculateSummaryStatistics().get(commandClass).getDbSelects();
    }
}
