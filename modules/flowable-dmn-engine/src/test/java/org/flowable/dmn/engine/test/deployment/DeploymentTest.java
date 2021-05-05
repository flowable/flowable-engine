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
package org.flowable.dmn.engine.test.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionTypes;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.h2.util.IOUtils;
import org.junit.Test;

public class DeploymentTest extends AbstractFlowableDmnTest {

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecision() {
        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");
        assertThat(decision.getDecisionType()).isEqualTo(DecisionTypes.DECISION_TABLE);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionTableDecision() {
        DmnDecision decision = repositoryService.createDecisionQuery()
            .latestVersion()
            .decisionKey("decision")
            .decisionType(DecisionTypes.DECISION_TABLE)
            .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionTableDecisionQueryWrongType() {
        DmnDecision decision = repositoryService.createDecisionQuery()
            .latestVersion()
            .decisionKey("decision")
            .decisionTypeLike("%service")
            .singleResult();
        assertThat(decision).isNull();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions_DMN12.dmn")
    public void deploySingleDecisionDMN12() {
        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionAndValidateCache() {
        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");

        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isTrue();
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isFalse();

        decision = repositoryService.getDecision(decision.getId());
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");
        assertThat(decision.getDecisionType()).isEqualTo(DecisionTypes.DECISION_TABLE);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionAndValidateVersioning() {
        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();

        assertThat(decision.getVersion()).isEqualTo(1);

        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .deploy();

        decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();

        assertThat(decision.getVersion()).isEqualTo(2);
    }

    @Test
    public void deploySingleDecisionInTenantAndValidateCache() throws Exception {
        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .decisionTenantId("testTenant")
                .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");
        assertThat(decision.getTenantId()).isEqualTo("testTenant");
        assertThat(decision.getVersion()).isEqualTo(1);

        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isTrue();
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isFalse();

        decision = repositoryService.getDecision(decision.getId());
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");

        deleteDeployments();
    }

    @Test
    public void deploySingleDecisionInTenantAndValidateVersioning() throws Exception {
        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .decisionTenantId("testTenant")
                .singleResult();

        assertThat(decision.getVersion()).isEqualTo(1);

        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .decisionTenantId("testTenant")
                .singleResult();

        assertThat(decision.getVersion()).isEqualTo(2);

        deleteDeployments();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_decisions.dmn")
    public void deployMultipleDecisions() throws Exception {
        DmnDecision decision = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision")
                .singleResult();
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");

        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isTrue();
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId())).isFalse();

        decision = repositoryService.getDecision(decision.getId());
        assertThat(decision).isNotNull();
        assertThat(decision.getKey()).isEqualTo("decision");

        DmnDecision decision2 = repositoryService.createDecisionQuery()
                .latestVersion()
                .decisionKey("decision2")
                .singleResult();
        assertThat(decision2).isNotNull();
        assertThat(decision2.getKey()).isEqualTo("decision2");

        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision2.getId())).isTrue();
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertThat(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision2.getId())).isFalse();

        decision2 = repositoryService.getDecision(decision2.getId());
        assertThat(decision2).isNotNull();
        assertThat(decision2.getKey()).isEqualTo("decision2");
    }

    @Test
    public void deployWithCategory() throws Exception {
        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("testTenant")
                .category("TEST_DEPLOYMENT_CATEGORY")
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().deploymentCategory("TEST_DEPLOYMENT_CATEGORY").singleResult();
        assertThat(deployment).isNotNull();

        DmnDecision decisionTable = repositoryService.createDecisionQuery().decisionKey("decision").singleResult();
        assertThat(decisionTable).isNotNull();

        repositoryService.setDecisionCategory(decisionTable.getId(), "TEST_DECISION_TABLE_CATEGORY");

        DmnDecision decisionWithCategory = repositoryService.createDecisionQuery().decisionCategory("TEST_DECISION_TABLE_CATEGORY").singleResult();
        assertThat(decisionWithCategory).isNotNull();

        deleteDeployments();
    }
    
    @Test
    public void multipleSameDeployments() throws Exception {
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .enableDuplicateFiltering()
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .singleResult();
        assertThat(deployment).isNotNull();
        
        List<DmnDecision> decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .list();
        assertThat(decisions).hasSize(1);

        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .enableDuplicateFiltering()
                .deploy();
        
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .list();
        assertThat(deployments).hasSize(1);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .list();
        assertThat(decisions).hasSize(1);

        deleteDeployments();
    }
    
    @Test
    public void multipleDeployments() throws Exception {
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .enableDuplicateFiltering()
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .singleResult();
        assertThat(deployment).isNotNull();
        
        List<DmnDecision> decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .list();
        assertThat(decisions).hasSize(1);

        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple2.dmn")
                .enableDuplicateFiltering()
                .deploy();
        
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .list();
        assertThat(deployments).hasSize(2);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("anotherDecision")
                .list();
        assertThat(decisions).hasSize(1);
        
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple2.dmn")
                .enableDuplicateFiltering()
                .deploy();
        
        deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .list();
        assertThat(deployments).hasSize(2);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("anotherDecision")
                .list();
        assertThat(decisions).hasSize(1);

        deleteDeployments();
    }
    
    @Test
    public void multipleSameDeploymentsInTenant() throws Exception {
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("myTenant")
                .enableDuplicateFiltering()
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .deploymentTenantId("myTenant")
                .singleResult();
        assertThat(deployment).isNotNull();
        
        List<DmnDecision> decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .decisionTenantId("myTenant")
                .list();
        assertThat(decisions).hasSize(1);

        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("myTenant")
                .enableDuplicateFiltering()
                .deploy();
        
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .deploymentTenantId("myTenant")
                .list();
        assertThat(deployments).hasSize(1);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .decisionTenantId("myTenant")
                .list();
        assertThat(decisions).hasSize(1);

        deleteDeployments();
    }
    
    @Test
    public void multipleDeploymentsInTenant() throws Exception {
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("myTenant")
                .enableDuplicateFiltering()
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .deploymentTenantId("myTenant")
                .singleResult();
        assertThat(deployment).isNotNull();
        
        List<DmnDecision> decisions = repositoryService.createDecisionQuery()
                .decisionKey("decision")
                .decisionTenantId("myTenant")
                .list();
        assertThat(decisions).hasSize(1);

        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple2.dmn")
                .tenantId("myTenant")
                .enableDuplicateFiltering()
                .deploy();
        
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .deploymentTenantId("myTenant")
                .list();
        assertThat(deployments).hasSize(2);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("anotherDecision")
                .decisionTenantId("myTenant")
                .list();
        assertThat(decisions).hasSize(1);
        
        repositoryService.createDeployment()
                .name("deploymentA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple2.dmn")
                .tenantId("myTenant")
                .enableDuplicateFiltering()
                .deploy();
        
        deployments = repositoryService.createDeploymentQuery()
                .deploymentName("deploymentA")
                .deploymentTenantId("myTenant")
                .list();
        assertThat(deployments).hasSize(2);
        
        decisions = repositoryService.createDecisionQuery()
                .decisionKey("anotherDecision")
                .decisionTenantId("myTenant")
                .list();
        assertThat(decisions).hasSize(1);

        deleteDeployments();
    }
    
    @Test
    public void deploySingleDecisionWithParentDeploymentId() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .parentDeploymentId("someDeploymentId")
                .deploy();
        
        org.flowable.dmn.api.DmnDeployment newDeployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .deploy();
        
        try {
            DmnDecision decision = repositoryService.createDecisionQuery().deploymentId(deployment.getId()).singleResult();
            assertThat(decision).isNotNull();
            assertThat(decision.getKey()).isEqualTo("decision");
            assertThat(decision.getVersion()).isEqualTo(1);

            DmnDecision newDecision = repositoryService.createDecisionQuery().deploymentId(newDeployment.getId()).singleResult();
            assertThat(newDecision).isNotNull();
            assertThat(newDecision.getKey()).isEqualTo("decision");
            assertThat(newDecision.getVersion()).isEqualTo(2);

            DecisionExecutionAuditContainer auditContainer = ruleService.createExecuteDecisionBuilder()
                    .decisionKey("decision")
                    .parentDeploymentId("someDeploymentId")
                    .executeWithAuditTrail();
            assertThat(auditContainer.getDecisionKey()).isEqualTo("decision");
            assertThat(auditContainer.getDecisionVersion()).isEqualTo(1);

            dmnEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(true);
            auditContainer = ruleService.createExecuteDecisionBuilder().decisionKey("decision").executeWithAuditTrail();
            assertThat(auditContainer.getDecisionKey()).isEqualTo("decision");
            assertThat(auditContainer.getDecisionVersion()).isEqualTo(2);
        } finally {
            dmnEngineConfiguration.setAlwaysLookupLatestDefinitionVersion(false);
            repositoryService.deleteDeployment(deployment.getId());
            repositoryService.deleteDeployment(newDeployment.getId());
        }
    }

    @Test
    @DmnDeployment
    public void testNativeQuery() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();

        long count = repositoryService.createNativeDeploymentQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(DmnDeploymentEntity.class) + " D1, "
                        + managementService.getTableName(DecisionEntity.class) + " D2 "
                        + "WHERE D1.ID_ = D2.DEPLOYMENT_ID_ "
                        + "AND D1.ID_ = #{deploymentId}")
                .parameter("deploymentId", deployment.getId())
                .count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/chapter11.dmn")
    public void testDecisionServicesDeployment() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();

        List<DmnDecision> decisionServices = repositoryService.createDecisionQuery().deploymentId(deployment.getId()).list();
        assertThat(decisionServices).hasSize(2);
        assertThat(decisionServices)
            .extracting(DmnDecision::getDecisionType)
            .containsExactly("decision_service", "decision_service");

        List<DmnDecision> decisionServices2 = repositoryService.createDecisionQuery().decisionType(DecisionTypes.DECISION_SERVICE).list();
        assertThat(decisionServices2).hasSize(2);

        List<DmnDecision> decisionTables = repositoryService.createDecisionQuery().decisionType(DecisionTypes.DECISION_TABLE).list();
        assertThat(decisionTables).isEmpty();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/chapter11_dmn13.dmn")
    public void testDecisionServicesDeploymentDMN13() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();

        List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
        // no diagram image because of missing DI info for some decision services
        assertThat(resourceNames).hasSize(1);

        List<DmnDecision> decisionServices = repositoryService.createDecisionQuery().deploymentId(deployment.getId()).list();
        assertThat(decisionServices).hasSize(10);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/decision_service_1.dmn")
    public void testDecisionServicesDI() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();

        List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
        assertThat(resourceNames).hasSize(2);

        String resourceName = "org/flowable/dmn/engine/test/deployment/decision_service_1.dmn";
        String diagramResourceName = "org/flowable/dmn/engine/test/deployment/decision_service_1.decisionServiceOne.png";
        assertThat(resourceNames).contains(resourceName, diagramResourceName);

        InputStream inputStream = repositoryService.getResourceAsStream(deployment.getId(), resourceName);
        assertThat(inputStream).isNotNull();
        IOUtils.closeSilently(inputStream);

        InputStream diagramInputStream = repositoryService.getResourceAsStream(deployment.getId(), diagramResourceName);
        assertThat(diagramInputStream).isNotNull();
        IOUtils.closeSilently(diagramInputStream);

        DmnDecision decision = repositoryService.createDecisionQuery().deploymentId(deployment.getId()).singleResult();

        InputStream caseDiagramInputStream = repositoryService.getDecisionRequirementsDiagram(decision.getId());
        assertThat(caseDiagramInputStream).isNotNull();
        IOUtils.closeSilently(caseDiagramInputStream);
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/decisionServiceMultipleOutputDecisions.dmn")
    public void testExportedDecisionServicesDeployment() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(deployment).isNotNull();

        List<DmnDecision> decisionServices = repositoryService.createDecisionQuery().deploymentId(deployment.getId()).list();
        assertThat(decisionServices).hasSize(1);
        assertThat(decisionServices.get(0).getDecisionType()).isEqualTo(DecisionTypes.DECISION_SERVICE);
    }
    
    @Test
    @DmnDeployment
    public void testDeployWithXmlSuffix() {
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    @Test
    public void testMixedDeployment() throws Exception {
        repositoryService.createDeployment().name("mixedDeployment")
            .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
            .addClasspathResource("org/flowable/dmn/engine/test/deployment/chapter11.dmn")
            .tenantId("testTenant")
            .deploy();

        DmnDecision decisionTable = repositoryService.createDecisionQuery().decisionType(DecisionTypes.DECISION_TABLE).singleResult();
        List<DmnDecision> decisionService = repositoryService.createDecisionQuery().decisionType(DecisionTypes.DECISION_SERVICE).list();
        assertThat(decisionTable.getKey()).isEqualTo("decision");
        assertThat(decisionService).extracting(DmnDecision::getKey)
            .containsExactly("_7befd964-eefa-4d8f-908d-8f6ad8d22c67", "_4d91e3a5-acec-4254-81e4-8535a1d336ee");

        deleteDeployments();
    }

    protected void deleteDeployments() {
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery().list();
        for (org.flowable.dmn.api.DmnDeployment deployment : deployments) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }
}
