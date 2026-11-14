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
import static org.flowable.common.engine.impl.persistence.deploy.AbstractSameDeploymentDefinitionLookup.parentDeploymentKey;

import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
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
        setCacheSameDeploymentDefinitionMisses(false);
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

        Map<String, Long> secondSelects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(), "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneProcessTaskCase")
                        .start());

        // The process definition comes from the same deployment cache, only the suspension check selects it by id
        assertThat(secondSelects).containsOnly(
                entry("selectEntityLinksByQuery", 2L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L),
                entry("selectById org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L)
        );
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(2);
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

        Map<String, Long> secondSelects = profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(), "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd",
                () -> cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneCaseTaskCase")
                        .start());

        // The cached case definition id is verified by selecting the case definition by id
        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectEntityLinksByQuery", 2L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 2L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").count()).isEqualTo(2);
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

        Map<String, Long> secondSelects = profileDatabaseSelects(processEngineConfiguration.getCommandExecutor(), "org.flowable.engine.impl.cmd.StartProcessInstanceCmd",
                () -> processEngineRuntimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("caseTask")
                        .start());

        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectEntityLinksByQuery", 2L),
                entry("selectLatestProcessDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L),
                entry("selectProcessDefinitionInfoByProcessDefinitionId", 1L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(2);
    }

    @Test
    public void testStartCaseWithParentDeploymentIdTwice() {
        CmmnDeployment deployment = deployCase("oneHumanTaskCase.cmmn", null, null);
        assertThat(deployment.getParentDeploymentId()).isEqualTo(deployment.getId());

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", deployment.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", deployment.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").count()).isEqualTo(2);
    }

    @Test
    public void testStartCaseWithParentDeploymentIdTwiceWithoutVerifyingCachedDefinitions() {
        cmmnEngineConfiguration.setVerifyCachedSameDeploymentDefinitions(false);
        try {
            CmmnDeployment deployment = deployCase("oneHumanTaskCase.cmmn", null, null);

            Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", deployment.getId(), null, false);
            assertThat(firstSelects).containsOnly(
                    entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                    entry("selectEntityLinksByQuery", 1L),
                    entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
            );

            Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", deployment.getId(), null, false);
            assertThat(secondSelects).containsOnly(
                    entry("selectEntityLinksByQuery", 1L),
                    entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
            );
            assertCaseInstancesDeployedTo("oneHumanTaskCase", 2, deployment.getId());
        } finally {
            cmmnEngineConfiguration.setVerifyCachedSameDeploymentDefinitions(true);
        }
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissForOtherTenantDoesNotHideDefinition() {
        setCacheSameDeploymentDefinitionMisses(true);
        CmmnDeployment parent = deployCase("oneHumanTaskCase.cmmn", null, null);

        // Start for tenant X, the parent has no case definition for that tenant and the fallback finds the one without tenant
        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "X", true);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectLatestCaseDefinitionByKeyAndTenantId", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("X"), 1, parent.getId());

        // Start without tenant, the miss for tenant X must not hide the case definition
        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").list())
                .hasSize(2)
                .extracting(caseInstance -> deploymentIdOf(caseInstance.getCaseDefinitionId()))
                .containsOnly(parent.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissTwice() {
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment latest = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 2, latest.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissTwiceWithCachedMisses() {
        setCacheSameDeploymentDefinitionMisses(true);
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment latest = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 2, latest.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissThenChildDeployed() {
        setCacheSameDeploymentDefinitionMisses(true);
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment latest = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, latest.getId());

        CmmnDeployment child = deployCase("oneHumanTaskCase.cmmn", parent.getId(), null);

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(child.getId()).singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").list())
                .extracting(caseInstance -> deploymentIdOf(caseInstance.getCaseDefinitionId()))
                .containsExactlyInAnyOrder(latest.getId(), child.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissPicksUpNewVersion() {
        startCaseWithParentDeploymentIdMissPicksUpNewVersion(false);
    }

    @Test
    public void testStartCaseWithParentDeploymentIdMissPicksUpNewVersionWithCachedMisses() {
        setCacheSameDeploymentDefinitionMisses(true);
        startCaseWithParentDeploymentIdMissPicksUpNewVersion(true);
    }

    protected void startCaseWithParentDeploymentIdMissPicksUpNewVersion(boolean cachedMisses) {
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment latest1 = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, latest1.getId());

        CmmnDeployment latest2 = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        if (cachedMisses) {
            assertThat(secondSelects).containsOnly(
                    entry("selectEntityLinksByQuery", 1L),
                    entry("selectLatestCaseDefinitionByKey", 1L),
                    entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
            );
        } else {
            assertThat(secondSelects).containsOnly(
                    entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                    entry("selectEntityLinksByQuery", 1L),
                    entry("selectLatestCaseDefinitionByKey", 1L),
                    entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
            );
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").list())
                .extracting(caseInstance -> deploymentIdOf(caseInstance.getCaseDefinitionId()))
                .containsExactlyInAnyOrder(latest1.getId(), latest2.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdAfterChildUndeployed() {
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment child = deployCase("oneHumanTaskCase.cmmn", parent.getId(), null);
        CmmnDeployment latest = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, child.getId());

        cmmnRepositoryService.deleteDeployment(child.getId(), true);

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(secondSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, latest.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdWithStaleCachedDefinition() {
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, null);
        CmmnDeployment child1 = deployCase("oneHumanTaskCase.cmmn", parent.getId(), null);

        startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, child1.getId());
        String cacheKey = parentDeploymentKey(parent.getId(), "oneHumanTaskCase");
        String staleCaseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(child1.getId()).singleResult().getId();
        assertThat(cmmnEngineConfiguration.getSameDeploymentDefinitionCache().get(cacheKey)).isEqualTo(staleCaseDefinitionId);

        cmmnRepositoryService.deleteDeployment(child1.getId(), true);
        CmmnDeployment child2 = deployCase("oneHumanTaskCase.cmmn", parent.getId(), null);

        // Simulate a node that did not see the undeploy and the deploy
        cmmnEngineConfiguration.getSameDeploymentDefinitionCache().add(cacheKey, staleCaseDefinitionId);

        Map<String, Long> selects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), null, false);
        assertThat(selects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectCaseDefinitionByParentDeploymentAndKey", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 1, child2.getId());
        String caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(child2.getId()).singleResult().getId();
        assertThat(cmmnEngineConfiguration.getSameDeploymentDefinitionCache().get(cacheKey)).isEqualTo(caseDefinitionId);
    }

    @Test
    public void testStartCaseWithParentDeploymentIdOtherTenant() {
        CmmnDeployment parent = deployCase("oneHumanTaskCase.cmmn", null, "tenantA");
        CmmnDeployment otherTenant = deployCase("oneHumanTaskCase.cmmn", null, "tenantB");

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "tenantA", false);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantA"), 1, parent.getId());

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "tenantB", false);
        assertThat(secondSelects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKeyAndTenantId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );

        Map<String, Long> thirdSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "tenantB", false);
        assertThat(thirdSelects).containsOnly(
                entry("selectById org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl", 1L),
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKeyAndTenantId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantB"), 2, otherTenant.getId());
    }

    @Test
    public void testStartCaseWithParentDeploymentIdDefaultTenantFallback() {
        CmmnDeployment parent = deployCase("oneServiceTask.cmmn", null, "tenantA");
        CmmnDeployment defaultTenant = deployCase("oneHumanTaskCase.cmmn", null, null);

        Map<String, Long> firstSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "tenantA", true);
        assertThat(firstSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectLatestCaseDefinitionByKeyAndTenantId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );

        Map<String, Long> secondSelects = startCaseWithParentDeploymentId("oneHumanTaskCase", parent.getId(), "tenantA", true);
        assertThat(secondSelects).containsOnly(
                entry("selectCaseDefinitionByParentDeploymentAndKeyAndTenantId", 1L),
                entry("selectEntityLinksByQuery", 1L),
                entry("selectLatestCaseDefinitionByKey", 1L),
                entry("selectLatestCaseDefinitionByKeyAndTenantId", 1L),
                entry("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", 1L)
        );
        assertCaseInstancesDeployedTo("oneHumanTaskCase", 2, defaultTenant.getId());
    }

    protected CmmnDeployment deployCase(String resource, String parentDeploymentId, String tenantId) {
        CmmnDeploymentBuilder builder = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/" + resource);
        if (parentDeploymentId != null) {
            builder.parentDeploymentId(parentDeploymentId);
        }
        if (tenantId != null) {
            builder.tenantId(tenantId);
        }
        return builder.deploy();
    }

    protected Map<String, Long> startCaseWithParentDeploymentId(String caseDefinitionKey, String parentDeploymentId, String tenantId,
            boolean fallbackToDefaultTenant) {
        return profileDatabaseSelects(cmmnEngineConfiguration.getCommandExecutor(), "org.flowable.cmmn.engine.impl.cmd.StartCaseInstanceCmd", () -> {
            CaseInstanceBuilder builder = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey(caseDefinitionKey)
                    .caseDefinitionParentDeploymentId(parentDeploymentId);
            if (tenantId != null) {
                builder.tenantId(tenantId);
            }
            if (fallbackToDefaultTenant) {
                builder.fallbackToDefaultTenant();
            }
            builder.start();
        });
    }

    protected String deploymentIdOf(String caseDefinitionId) {
        CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(caseDefinitionId);
        return caseDefinition.getDeploymentId();
    }

    protected void assertCaseInstancesDeployedTo(String caseDefinitionKey, int expectedCount, String expectedDeploymentId) {
        assertCaseInstancesDeployedTo(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey(caseDefinitionKey), expectedCount, expectedDeploymentId);
    }

    protected void assertCaseInstancesDeployedTo(CaseInstanceQuery query, int expectedCount, String expectedDeploymentId) {
        assertThat(query.list())
                .hasSize(expectedCount)
                .extracting(caseInstance -> deploymentIdOf(caseInstance.getCaseDefinitionId()))
                .containsOnly(expectedDeploymentId);
    }

    protected void setCacheSameDeploymentDefinitionMisses(boolean cacheSameDeploymentDefinitionMisses) {
        cmmnEngineConfiguration.setCacheSameDeploymentDefinitionMisses(cacheSameDeploymentDefinitionMisses);
        processEngineConfiguration.setCacheSameDeploymentDefinitionMisses(cacheSameDeploymentDefinitionMisses);
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
