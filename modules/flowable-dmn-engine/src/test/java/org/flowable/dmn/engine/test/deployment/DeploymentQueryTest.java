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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentQueryTest extends AbstractFlowableDmnTest {
    
    private String deploymentId1;
    private String deploymentId2;
    private String deploymentId3;
    
    @Before
    public void deploy() {
        deploymentId1 = repositoryService.createDeployment()
                .name("test1.dmn")
                .category("testCategoryA")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple.dmn")
                .tenantId("tenantA")
                .deploy().getId();
        
        deploymentId2 = repositoryService.createDeployment()
                .name("test2.dmn")
                .category("testCategoryB")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple2.dmn")
                .tenantId("tenantA")
                .deploy().getId();
        
        deploymentId3 = repositoryService.createDeployment()
                .name("test3.dmn")
                .category("testCategoryC")
                .addClasspathResource("org/flowable/dmn/engine/test/deployment/simple3.dmn")
                .tenantId("tenantB")
                .deploy().getId();
    }
    
    @After
    public void cleanup() {
        repositoryService.deleteDeployment(deploymentId1);
        repositoryService.deleteDeployment(deploymentId2);
        repositoryService.deleteDeployment(deploymentId3);
    }
    
    @Test
    public void testQueryById() {
        assertNotNull(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentId(deploymentId1).list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentId(deploymentId1).count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentId("invalid").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentId("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentId("invalid").count());
    }
    
    @Test
    public void testQueryByName() {
        assertNotNull(repositoryService.createDeploymentQuery().deploymentName("test2.dmn").singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentName("test2.dmn").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentName("test2.dmn").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentName("invalid").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentName("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentName("invalid").count());
    }
    
    @Test
    public void testQueryByNameLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentNameLike("test%").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentNameLike("test%").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentNameLike("inva%").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentNameLike("inva").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentNameLike("inva").count());
    }
    
    @Test
    public void testQueryByCategory() {
        assertNotNull(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentCategory("inva%").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentCategory("inva%").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentCategory("inva%").count());
    }
    
    @Test
    public void testQueryByCategoryNotEquals() {
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").list().size());
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").count());
        
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").count());
    }
    
    @Test
    public void testQueryByTenantId() {
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").list().size());
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantId("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantId("invalid").count());
    }
    
    @Test
    public void testQueryByTenantIdLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").count());
    }
    
    @Test
    public void testQueryByDecisionTableKey() {
        assertEquals(1, repositoryService.createDeploymentQuery().decisionTableKey("anotherDecision").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().decisionTableKey("anotherDecision").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().decisionTableKey("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().decisionTableKey("invalid").count());
    }
    
    @Test
    public void testQueryByDecisionTableKeyLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().decisionTableKeyLike("%sion").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().decisionTableKeyLike("%sion").count());
        assertEquals(1, repositoryService.createDeploymentQuery().decisionTableKeyLike("%sion").listPage(0, 1).size());
        assertEquals(2, repositoryService.createDeploymentQuery().decisionTableKeyLike("%sion").listPage(0, 2).size());
        assertEquals(2, repositoryService.createDeploymentQuery().decisionTableKeyLike("%sion").listPage(1, 2).size());
        
        assertEquals(0, repositoryService.createDeploymentQuery().decisionTableKeyLike("inva%").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().decisionTableKeyLike("inva%").count());
    }
    
}
