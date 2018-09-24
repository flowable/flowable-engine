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
package org.flowable.cmmn.test.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.h2.util.IOUtils;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentTest extends FlowableCmmnTestCase {
    
    /**
     * Simplest test possible: deploy the simple-case.cmmn (from the cmmn-converter module) and see if 
     * - a deployment exists
     * - a resouce exists
     * - a case definition was created 
     * - that case definition is in the cache
     * - case definition properties set
     */
    @Test
    @CmmnDeployment
    public void testCaseDefinitionDeployed() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertNotNull(cmmnDeployment);
        
        List<String> resourceNames = cmmnRepositoryService.getDeploymentResourceNames(cmmnDeployment.getId());
        assertEquals(1, resourceNames.size());
        assertEquals("org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDeployed.cmmn", resourceNames.get(0));
        
        InputStream inputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), resourceNames.get(0));
        assertNotNull(inputStream);
        inputStream.close();
        
        DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache = cmmnEngineConfiguration.getCaseDefinitionCache();
        assertEquals(1, ((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll().size());
        
        CaseDefinitionCacheEntry cachedCaseDefinition = ((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll().iterator().next();
        assertNotNull(cachedCaseDefinition.getCase());
        assertNotNull(cachedCaseDefinition.getCmmnModel());
        assertNotNull(cachedCaseDefinition.getCaseDefinition());
        
        CaseDefinition caseDefinition = cachedCaseDefinition.getCaseDefinition();
        assertNotNull(caseDefinition.getId());
        assertNotNull(caseDefinition.getDeploymentId());
        assertNotNull(caseDefinition.getKey());
        assertNotNull(caseDefinition.getResourceName());
        assertTrue(caseDefinition.getVersion() > 0);
        
        caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(cmmnDeployment.getId()).singleResult();
        assertNotNull(caseDefinition.getId());
        assertNotNull(caseDefinition.getDeploymentId());
        assertNotNull(caseDefinition.getKey());
        assertNotNull(caseDefinition.getResourceName());
        assertEquals(1, caseDefinition.getVersion());
        
        CmmnModel cmmnModel = cmmnRepositoryService.getCmmnModel(caseDefinition.getId());
        assertNotNull(cmmnModel);
        
        // CmmnParser should have added behavior to plan items
        for (PlanItem planItem : cmmnModel.getPrimaryCase().getPlanModel().getPlanItems()) {
            assertNotNull(planItem.getBehavior());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testCaseDefinitionDI() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertNotNull(cmmnDeployment);
        
        List<String> resourceNames = cmmnRepositoryService.getDeploymentResourceNames(cmmnDeployment.getId());
        assertEquals(2, resourceNames.size());
        
        String resourceName = "org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDI.cmmn";
        String diagramResourceName = "org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDI.caseB.png";
        assertTrue(resourceNames.contains(resourceName));
        assertTrue(resourceNames.contains(diagramResourceName));
        
        InputStream inputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), resourceName);
        assertNotNull(inputStream);
        IOUtils.closeSilently(inputStream);
        
        InputStream diagramInputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), diagramResourceName);
        assertNotNull(diagramInputStream);
        IOUtils.closeSilently(diagramInputStream);
        
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(cmmnDeployment.getId()).singleResult();
        
        InputStream caseDiagramInputStream = cmmnRepositoryService.getCaseDiagram(caseDefinition.getId());
        assertNotNull(caseDiagramInputStream);
        IOUtils.closeSilently(caseDiagramInputStream);
    }

    @Test
    public void testBulkInsertCmmnDeployments() {

        List<String> deploymentIds = cmmnEngineConfiguration.getCommandExecutor()
            .execute(commandContext -> {
                org.flowable.cmmn.api.repository.CmmnDeployment deployment1 = cmmnRepositoryService.createDeployment()
                    .name("First deployment")
                    .key("one-human")
                    .category("test")
                    .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                    .deploy();
                org.flowable.cmmn.api.repository.CmmnDeployment deployment2 = cmmnRepositoryService.createDeployment()
                    .name("Second deployment")
                    .key("example-task")
                    .addClasspathResource("org/flowable/cmmn/test/example-task-model.cmmn")
                    .deploy();

                return Arrays.asList(deployment1.getId(), deployment2.getId());
            });

        assertThat(cmmnRepositoryService.getDeploymentResourceNames(deploymentIds.get(0)))
            .containsExactlyInAnyOrder("org/flowable/cmmn/test/one-human-task-model.cmmn");

        assertThat(cmmnRepositoryService.getDeploymentResourceNames(deploymentIds.get(1)))
            .containsExactlyInAnyOrder("org/flowable/cmmn/test/example-task-model.cmmn");

        assertThat(cmmnRepositoryService.createDeploymentQuery().list())
            .as("Deployment time not null")
            .allSatisfy(deployment -> assertThat(deployment.getDeploymentTime()).as(deployment.getName()).isNotNull())
            .extracting(org.flowable.cmmn.api.repository.CmmnDeployment::getId, org.flowable.cmmn.api.repository.CmmnDeployment::getName,
                org.flowable.cmmn.api.repository.CmmnDeployment::getKey, org.flowable.cmmn.api.repository.CmmnDeployment::getCategory)
            .as("id, name, key, category")
            .containsExactlyInAnyOrder(
                tuple(deploymentIds.get(0), "First deployment", "one-human", "test"),
                tuple(deploymentIds.get(1), "Second deployment", "example-task", null)
            );

        deploymentIds.forEach(deploymentId -> cmmnRepositoryService.deleteDeployment(deploymentId, true));
    }
}
