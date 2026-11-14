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
import static org.flowable.common.engine.impl.persistence.deploy.AbstractSameDeploymentDefinitionLookup.parentDeploymentKey;

import java.util.List;
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
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.ExecuteDecisionBuilder;
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

    protected static final String KEY_RESOURCE = "org/flowable/dmn/engine/test/deployment/simple.dmn";
    protected static final String OTHER_RESOURCE = "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn";

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected DmnEngineConfiguration dmnEngineConfiguration;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected HistoryService historyService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected DmnRepositoryService dmnRepositoryService;
    protected DmnDecisionService dmnDecisionService;

    protected DbSqlSessionFactory oldDbSqlSessionFactory;

    @BeforeEach
    protected void setUp(ProcessEngine processEngine) {
        this.processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        this.cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
        this.dmnEngineConfiguration = (DmnEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        this.repositoryService = processEngine.getRepositoryService();
        this.runtimeService = processEngine.getRuntimeService();
        this.historyService = processEngine.getHistoryService();
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.dmnRepositoryService = dmnEngineConfiguration.getDmnRepositoryService();
        this.dmnDecisionService = dmnEngineConfiguration.getDmnDecisionService();

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
        setCacheSameDeploymentDefinitionMisses(false);
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

        Map<String, Long> secondSelects = profileDatabaseSelects(processEngineConfiguration.getCommandExecutor(),
                "org.flowable.engine.impl.cmd.StartProcessInstanceCmd",
                () -> runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("oneDecisionTaskProcess")
                        .variable("inputVariable1", 1)
                        .start());

        // The cached decision id is verified by selecting the decision by id
        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L),
                entry("selectChildExecutionsByProcessInstanceId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectExecutionsByParentExecutionId", 2L),
                entry("selectLatestProcessDefinitionByKey", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L),
                entry("selectVariablesByQuery", 1L)
        );
        assertThat(historyService.createHistoricVariableInstanceQuery().variableName("outputVariable1").list())
                .extracting(HistoricVariableInstance::getValue)
                .containsExactly("result1", "result1");
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

        Map<String, Long> secondSelects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(),
                "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneDecisionTaskCase")
                        .variable("testVar", "test2")
                        .start());

        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L),
                entry("selectEventSubscriptionsByScopeIdAndType", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        List<CaseInstance> caseInstances = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneDecisionTaskCase").list();
        assertThat(caseInstances).hasSize(2);
        assertThat(caseInstances)
                .extracting(instance -> cmmnRuntimeService.getVariable(instance.getId(), "resultVar"))
                .containsExactly("executed", "executed");
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

    @Test
    public void testExecuteDecisionWithParentDeploymentIdTwice() {
        DmnDeployment deployment = deployDecision(KEY_RESOURCE, null, null);
        assertThat(deployment.getParentDeploymentId()).isEqualTo(deployment.getId());

        DecisionExecution first = executeWithParent(deployment.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(first, deployment);

        DecisionExecution second = executeWithParent(deployment.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L)
        );
        assertExecuted(second, deployment);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdTwiceWithoutVerifyingCachedDefinitions() {
        dmnEngineConfiguration.setVerifyCachedSameDeploymentDefinitions(false);
        try {
            DmnDeployment deployment = deployDecision(KEY_RESOURCE, null, null);

            DecisionExecution first = executeWithParent(deployment.getId(), null, false);
            assertThat(first.selects).containsOnly(
                    entry("selectDecisionByDeploymentAndKey", 1L),
                    entry("selectDmnDeploymentsByQueryCriteria", 1L)
            );
            assertExecuted(first, deployment);

            DecisionExecution second = executeWithParent(deployment.getId(), null, false);
            assertThat(second.selects).isEmpty();
            assertExecuted(second, deployment);
        } finally {
            dmnEngineConfiguration.setVerifyCachedSameDeploymentDefinitions(true);
        }
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissForOtherTenantDoesNotHideDefinition() {
        setCacheSameDeploymentDefinitionMisses(true);
        DmnDeployment parent = deployDecision(KEY_RESOURCE, null, null);

        // Execute for tenant X, the parent has no decision for that tenant and the fallback finds the one without tenant
        DecisionExecution first = executeWithParent(parent.getId(), "X", true);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L),
                entry("selectLatestDecisionByKeyAndTenantId", 1L)
        );
        assertExecuted(first, parent);

        // Execute without tenant, the miss for tenant X must not hide the decision
        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(second, parent);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissTwice() {
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest);

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissTwiceWithCachedMisses() {
        setCacheSameDeploymentDefinitionMisses(true);
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest);

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissThenChildDeployed() {
        setCacheSameDeploymentDefinitionMisses(true);
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest);

        // Only the first deployment with the parent id is checked and that is the parent itself, so the child is not found
        deployDecision(KEY_RESOURCE, parent.getId(), null);
        DmnDeployment latest2 = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest2);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissPicksUpNewVersion() {
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment latest1 = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest1);

        DmnDeployment latest2 = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest2);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdMissPicksUpNewVersionWithCachedMisses() {
        setCacheSameDeploymentDefinitionMisses(true);
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment latest1 = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest1);

        DmnDeployment latest2 = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest2);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdAfterChildUndeployed() {
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, null);
        DmnDeployment child = deployDecision(KEY_RESOURCE, parent.getId(), null);
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        // Only the first deployment with the parent id is checked and that is the parent itself, so the child is not found
        DecisionExecution first = executeWithParent(parent.getId(), null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest);

        dmnRepositoryService.deleteDeployment(child.getId());

        DecisionExecution second = executeWithParent(parent.getId(), null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(second, latest);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdOtherTenant() {
        DmnDeployment parent = deployDecision(KEY_RESOURCE, null, "tenantA");
        deployDecision(KEY_RESOURCE, null, "tenantB");
        DmnDeployment tenantBLatest = deployDecision(KEY_RESOURCE, null, "tenantB");

        DecisionExecution first = executeWithParent(parent.getId(), "tenantA", false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(first, parent);

        DecisionExecution second = executeWithParent(parent.getId(), "tenantB", false);
        assertThat(second.selects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L),
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKeyAndTenantId", 1L)
        );
        assertExecuted(second, tenantBLatest);

        DecisionExecution third = executeWithParent(parent.getId(), "tenantB", false);
        assertThat(third.selects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L),
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKeyAndTenantId", 1L)
        );
        assertExecuted(third, tenantBLatest);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdDefaultTenantFallback() {
        DmnDeployment parent = deployDecision(OTHER_RESOURCE, null, "tenantA");
        DmnDeployment noTenant = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(parent.getId(), "tenantA", true);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L),
                entry("selectLatestDecisionByKeyAndTenantId", 1L)
        );
        assertExecuted(first, noTenant);

        DecisionExecution second = executeWithParent(parent.getId(), "tenantA", true);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKeyAndTenantId", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L),
                entry("selectLatestDecisionByKeyAndTenantId", 1L)
        );
        assertExecuted(second, noTenant);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdExternalParentAfterChildUndeployed() {
        String externalParentId = "externalParent";
        DmnDeployment child = deployDecision(KEY_RESOURCE, externalParentId, null);
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(externalParentId, null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(first, child);

        DecisionExecution second = executeWithParent(externalParentId, null, false);
        assertThat(second.selects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L)
        );
        assertExecuted(second, child);

        dmnRepositoryService.deleteDeployment(child.getId());

        DecisionExecution third = executeWithParent(externalParentId, null, false);
        assertThat(third.selects).containsOnly(
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(third, latest);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdExternalParentMissThenChildDeployed() {
        setCacheSameDeploymentDefinitionMisses(true);
        String externalParentId = "externalParent";
        DmnDeployment latest = deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution first = executeWithParent(externalParentId, null, false);
        assertThat(first.selects).containsOnly(
                entry("selectDmnDeploymentsByQueryCriteria", 1L),
                entry("selectLatestDecisionByKey", 1L)
        );
        assertExecuted(first, latest);

        DmnDeployment child = deployDecision(KEY_RESOURCE, externalParentId, null);
        deployDecision(KEY_RESOURCE, null, null);

        DecisionExecution second = executeWithParent(externalParentId, null, false);
        assertThat(second.selects).containsOnly(
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(second, child);
    }

    @Test
    public void testExecuteDecisionWithParentDeploymentIdExternalParentWithStaleCachedDefinition() {
        String externalParentId = "externalParent";
        DmnDeployment child1 = deployDecision(KEY_RESOURCE, externalParentId, null);

        DecisionExecution first = executeWithParent(externalParentId, null, false);
        assertExecuted(first, child1);
        String cacheKey = parentDeploymentKey(externalParentId, "decision1");
        String staleDecisionId = dmnRepositoryService.createDecisionQuery().deploymentId(child1.getId()).singleResult().getId();
        assertThat(dmnEngineConfiguration.getSameDeploymentDefinitionCache().get(cacheKey)).isEqualTo(staleDecisionId);

        dmnRepositoryService.deleteDeployment(child1.getId());
        DmnDeployment child2 = deployDecision(KEY_RESOURCE, externalParentId, null);

        // Simulate a node that did not see the undeploy and the deploy
        dmnEngineConfiguration.getSameDeploymentDefinitionCache().add(cacheKey, staleDecisionId);

        DecisionExecution second = executeWithParent(externalParentId, null, false);
        assertThat(second.selects).containsOnly(
                entry("selectById org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityImpl", 1L),
                entry("selectDecisionByDeploymentAndKey", 1L),
                entry("selectDmnDeploymentsByQueryCriteria", 1L)
        );
        assertExecuted(second, child2);
        String decisionId = dmnRepositoryService.createDecisionQuery().deploymentId(child2.getId()).singleResult().getId();
        assertThat(dmnEngineConfiguration.getSameDeploymentDefinitionCache().get(cacheKey)).isEqualTo(decisionId);
    }

    protected void setCacheSameDeploymentDefinitionMisses(boolean cacheSameDeploymentDefinitionMisses) {
        dmnEngineConfiguration.setCacheSameDeploymentDefinitionMisses(cacheSameDeploymentDefinitionMisses);
    }

    protected DmnDeployment deployDecision(String resource, String parentDeploymentId, String tenantId) {
        DmnDeploymentBuilder builder = dmnRepositoryService.createDeployment().addClasspathResource(resource);
        if (parentDeploymentId != null) {
            builder.parentDeploymentId(parentDeploymentId);
        }
        if (tenantId != null) {
            builder.tenantId(tenantId);
        }
        return builder.deploy();
    }

    protected DecisionExecution executeWithParent(String parentDeploymentId, String tenantId, boolean fallbackToDefaultTenant) {
        DecisionExecution execution = new DecisionExecution();
        execution.selects = profileDatabaseSelects(dmnEngineConfiguration.getCommandExecutor(),
                "org.flowable.dmn.engine.impl.cmd.EvaluateDecisionCmd",
                () -> {
                    ExecuteDecisionBuilder builder = dmnDecisionService.createExecuteDecisionBuilder()
                            .decisionKey("decision1")
                            .parentDeploymentId(parentDeploymentId)
                            .variable("inputVariable1", 1);
                    if (tenantId != null) {
                        builder.tenantId(tenantId);
                    }
                    if (fallbackToDefaultTenant) {
                        builder.fallbackToDefaultTenant();
                    }
                    execution.audit = builder.executeWithAuditTrail();
                });
        return execution;
    }

    protected void assertExecuted(DecisionExecution execution, DmnDeployment expectedDeployment) {
        DmnDecision expectedDecision = dmnRepositoryService.createDecisionQuery().deploymentId(expectedDeployment.getId()).singleResult();
        assertThat(execution.audit.getDecisionVersion()).isEqualTo(expectedDecision.getVersion());
        assertThat(execution.audit.getDecisionResult()).containsExactly(Map.of("outputVariable1", "result1"));
    }

    protected static class DecisionExecution {

        protected Map<String, Long> selects;
        protected DecisionExecutionAuditContainer audit;
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
