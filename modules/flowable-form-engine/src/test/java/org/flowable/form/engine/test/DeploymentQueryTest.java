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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentQueryTest extends AbstractFlowableFormTest {
    
    private String deploymentId1;
    private String deploymentId2;
    private String deploymentId3;
    
    private String formModel = "{\n" + 
            "    \"key\": \"$changeme$\",\n" + 
            "    \"name\": \"My first form\",\n" + 
            "    \"fields\": [\n" + 
            "        {\n" + 
            "            \"id\": \"input1\",\n" + 
            "            \"name\": \"Input1\",\n" + 
            "            \"type\": \"text\",\n" + 
            "            \"required\": false,\n" + 
            "            \"placeholder\": \"empty\"\n" + 
            "        }\n" + 
            "    ]\n" + 
            "}\n" + 
            "";
    
    @BeforeEach
    public void deploy() {
        deploymentId1 = repositoryService.createDeployment()
                .name("test1.form")
                .category("testCategoryA")
                .addString("form1.form", formModel.replace("$changeme$", "form1"))
                .tenantId("tenantA")
                .deploy().getId();
        
        deploymentId2 = repositoryService.createDeployment()
                .name("test2.form")
                .category("testCategoryB")
                .addString("form2.form", formModel.replace("$changeme$", "form2"))
                .tenantId("tenantA")
                .deploy().getId();
        
        deploymentId3 = repositoryService.createDeployment()
                .name("test3.form")
                .category("testCategoryC")
                .addString("form3.form", formModel.replace("$changeme$", "form3"))
                .tenantId("tenantB")
                .deploy().getId();
    }
    
    @AfterEach
    public void cleanup() {
        repositoryService.deleteDeployment(deploymentId1, true);
        repositoryService.deleteDeployment(deploymentId2, true);
        repositoryService.deleteDeployment(deploymentId3, true);
    }
    
    @Test
    public void testQueryById() {
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).count()).isOne();
        
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByName() {
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.form").singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.form").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.form").count()).isOne();
        
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
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").count()).isOne();
        
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
    public void testQueryByDecisionTableKey() {
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKey("form2").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKey("form2").count()).isOne();
        
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKey("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKey("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDecisionTableKeyLike() {
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("form%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("form%").count()).isEqualTo(3);
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("form%").listPage(0, 1)).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("form%").listPage(0, 2)).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("form%").listPage(1, 2)).hasSize(2);
        
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().formDefinitionKeyLike("inva%").count()).isZero();
    }
    
}
