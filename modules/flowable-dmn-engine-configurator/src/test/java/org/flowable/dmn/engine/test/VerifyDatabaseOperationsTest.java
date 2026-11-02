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
package org.flowable.dmn.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.FlowableTest;
import org.flowable.engine.test.profiler.FlowableProfiler;
import org.flowable.engine.test.profiler.ProfilingDbSqlSessionFactory;
import org.flowable.engine.test.profiler.TotalExecutionTimeCommandInterceptor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * @author Filip Hrisafov
 */
@FlowableTest
@DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
public class VerifyDatabaseOperationsTest {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected HistoryService historyService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected DmnRepositoryService dmnRepositoryService;

    protected DbSqlSessionFactory oldDbSqlSessionFactory;

    @BeforeEach
    protected void setUp(ProcessEngine processEngine) {
        this.processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        this.cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        DmnEngineConfiguration dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        this.repositoryService = processEngine.getRepositoryService();
        this.runtimeService = processEngine.getRuntimeService();
        this.historyService = processEngine.getHistoryService();
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.dmnRepositoryService = dmnEngineConfiguration.getDmnRepositoryService();

        // The process, CMMN and DMN engine share the session factories
        this.oldDbSqlSessionFactory = processEngineConfiguration.getDbSqlSessionFactory();
        DbSqlSessionFactory newDbSqlSessionFactory = new ProfilingDbSqlSessionFactory(processEngineConfiguration.isUsePrefixId());
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
        processEngineConfiguration.addSessionFactory(newDbSqlSessionFactory);
    }

    @AfterEach
    protected void tearDown() {
        processEngineConfiguration.addSessionFactory(oldDbSqlSessionFactory);
        FlowableProfiler.getInstance().reset();

        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        for (CmmnDeployment deployment : cmmnRepositoryService.createDeploymentQuery().list()) {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
        }
        for (DmnDeployment deployment : dmnRepositoryService.createDeploymentQuery().list()) {
            dmnRepositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    public void testProcessWithDecisionTaskFromSameDeployment() {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeployment.bpmn20.xml")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .deploy();

        Map<String, Long> selects = profileDatabaseSelects(processEngineConfiguration.getCommandExecutor(),
                "org.flowable.engine.impl.cmd.StartProcessInstanceCmd",
                () -> runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("oneDecisionTaskProcess")
                        .variable("inputVariable1", 1)
                        .start());

        // The cached process definition has the parent deployment id, so the process deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectLatestProcessDefinitionByKey", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectExecutionsByParentExecutionId", 2L),
                entry("selectChildExecutionsByProcessInstanceId", 1L),
                entry("selectVariablesByQuery", 1L),
                entry("selectEntityLinksByQuery", 1L)
        );
        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("outputVariable1").singleResult())
                .extracting(HistoricVariableInstance::getValue)
                .isEqualTo("result1");
    }

    @Test
    public void testCaseWithDecisionTaskFromSameDeployment() {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/oneDecisionTaskCaseSameDeployment.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn")
                .deploy();

        Map<String, Long> selects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(),
                "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneDecisionTaskCase")
                        .variable("testVar", "test2")
                        .start());

        // The cached case definition has the parent deployment id, so the CMMN deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L),
                entry("selectEventSubscriptionsByScopeIdAndType", 1L)
        );
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneDecisionTaskCase").singleResult();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVar")).isEqualTo("executed");
    }

    @Test
    public void testGetDecisionsForProcessDefinition() {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/oneDecisionTaskProcessSameDeployment.bpmn20.xml")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneDecisionTaskProcess").singleResult();

        Map<String, Long> selects = profileDatabaseSelects(processEngineConfiguration.getCommandExecutor(),
                "org.flowable.engine.impl.cmd.GetDecisionsForProcessDefinitionCmd",
                () -> assertThat(repositoryService.getDecisionsForProcessDefinition(processDefinition.getId()))
                        .extracting(DmnDecision::getKey)
                        .containsExactly("decision1"));

        // The cached process definition has the parent deployment id, so the process deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectDecisionsByQueryCriteria", 1L)
        );
    }

    @Test
    public void testGetDecisionsForCaseDefinition() {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/oneDecisionTaskCaseSameDeployment.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn")
                .deploy();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneDecisionTaskCase").singleResult();

        Map<String, Long> selects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(),
                "org.flowable.cmmn.engine.impl.cmd.GetDecisionsForCaseDefinitionCmd",
                () -> assertThat(cmmnRepositoryService.getDecisionsForCaseDefinition(caseDefinition.getId()))
                        .extracting(DmnDecision::getKey)
                        .containsExactly("DecisionTable"));

        // The cached case definition has the parent deployment id, so the CMMN deployment is not selected
        assertThat(selects).containsOnly(
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectDecisionsByQueryCriteria", 1L)
        );
    }

    protected Map<String, Long> profileDatabaseSelects(CommandExecutor commandExecutor, String commandClass, Runnable runnable) {
        // Only the outer command is profiled, the commands of the other engines reuse its command context
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
