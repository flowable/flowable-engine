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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.Test;

public class DeploymentTest extends AbstractFlowableDmnTest {

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecision() {
        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .singleResult();
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionAndValidateCache() {
        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .singleResult();
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());

        assertTrue(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertFalse(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));

        decision = repositoryService.getDecisionTable(decision.getId());
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
    public void deploySingleDecisionAndValidateVersioning() {
        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .singleResult();

        assertEquals(1, decision.getVersion());

        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .deploy();

        decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .singleResult();

        assertEquals(2, decision.getVersion());
    }

    @Test
    public void deploySingleDecisionInTenantAndValidateCache() throws Exception {
        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .decisionTableTenantId("testTenant")
                .singleResult();
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());
        assertEquals("testTenant", decision.getTenantId());
        assertEquals(1, decision.getVersion());

        assertTrue(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertFalse(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));

        decision = repositoryService.getDecisionTable(decision.getId());
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());

        deleteDeployments();
    }

    @Test
    public void deploySingleDecisionInTenantAndValidateVersioning() throws Exception {
        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .decisionTableTenantId("testTenant")
                .singleResult();

        assertEquals(1, decision.getVersion());

        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/multiple_conclusions.dmn")
                .tenantId("testTenant")
                .deploy();

        decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .decisionTableTenantId("testTenant")
                .singleResult();

        assertEquals(2, decision.getVersion());

        deleteDeployments();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/multiple_decisions.dmn")
    public void deployMultipleDecisions() throws Exception {

        DmnDecisionTable decision = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision")
                .singleResult();
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());

        assertTrue(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertFalse(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision.getId()));

        decision = repositoryService.getDecisionTable(decision.getId());
        assertNotNull(decision);
        assertEquals("decision", decision.getKey());

        DmnDecisionTable decision2 = repositoryService.createDecisionTableQuery()
                .latestVersion()
                .decisionTableKey("decision2")
                .singleResult();
        assertNotNull(decision2);
        assertEquals("decision2", decision2.getKey());

        assertTrue(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision2.getId()));
        dmnEngineConfiguration.getDeploymentManager().getDecisionCache().clear();
        assertFalse(dmnEngineConfiguration.getDeploymentManager().getDecisionCache().contains(decision2.getId()));

        decision2 = repositoryService.getDecisionTable(decision2.getId());
        assertNotNull(decision2);
        assertEquals("decision2", decision2.getKey());
    }

    @Test
    public void deployWithCategory() throws Exception {

        repositoryService.createDeployment().name("secondDeployment")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("testTenant")
                .category("TEST_DEPLOYMENT_CATEGORY")
                .deploy();

        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().deploymentCategory("TEST_DEPLOYMENT_CATEGORY").singleResult();
        assertNotNull(deployment);

        DmnDecisionTable decisionTable = repositoryService.createDecisionTableQuery().decisionTableKey("decision").singleResult();
        assertNotNull(decisionTable);

        repositoryService.setDecisionTableCategory(decisionTable.getId(), "TEST_DECISION_TABLE_CATEGORY");

        DmnDecisionTable decisionTableWithCategory = repositoryService.createDecisionTableQuery().decisionTableCategory("TEST_DECISION_TABLE_CATEGORY").singleResult();
        assertNotNull(decisionTableWithCategory);

        deleteDeployments();
    }

    @Test
    @DmnDeployment
    public void testNativeQuery() {
        org.flowable.dmn.api.DmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(deployment);

        long count = repositoryService.createNativeDeploymentQuery()
                .sql("SELECT count(*) FROM " + managementService.getTableName(DmnDeploymentEntity.class) + " D1, "
                        + managementService.getTableName(DecisionTableEntity.class) + " D2 "
                        + "WHERE D1.ID_ = D2.DEPLOYMENT_ID_ "
                        + "AND D1.ID_ = #{deploymentId}")
                .parameter("deploymentId", deployment.getId())
                .count();

        assertEquals(2, count);
    }
    
    @Test
    @DmnDeployment
    public void testDeployWithXmlSuffix() {
        assertEquals(1, repositoryService.createDeploymentQuery().count());
    }

    protected void deleteDeployments() {
        List<org.flowable.dmn.api.DmnDeployment> deployments = repositoryService.createDeploymentQuery().list();
        for (org.flowable.dmn.api.DmnDeployment deployment : deployments) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }
}
