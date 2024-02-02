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
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByName() {
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.dmn").singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.dmn").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.dmn").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByNameLike() {
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("test%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("test%").count()).isEqualTo(3);

        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva%").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva").count()).isZero();
    }
    
    @Test
    public void testQueryByCategory() {
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").count()).isZero();
    }
    
    @Test
    public void testQueryByCategoryNotEquals() {
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").list()).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").count()).isEqualTo(2);

        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").count()).isEqualTo(3);
    }
    
    @Test
    public void testQueryByTenantId() {
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").list()).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").count()).isEqualTo(2);

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByTenantIdLike() {
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").count()).isEqualTo(3);

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDecisionKey() {
        assertThat(repositoryService.createDeploymentQuery().decisionKey("anotherDecision").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().decisionKey("anotherDecision").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().decisionKey("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().decisionKey("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDecisionKeyLike() {
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("%sion").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("%sion").count()).isEqualTo(3);
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("%sion").listPage(0, 1)).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("%sion").listPage(0, 2)).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("%sion").listPage(1, 2)).hasSize(2);

        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().decisionKeyLike("inva%").count()).isZero();
    }
    
}
