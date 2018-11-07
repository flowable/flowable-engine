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
package org.flowable.cmmn.test.tenant;

import static org.junit.Assert.assertEquals;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class MultiTenantTest extends FlowableCmmnTestCase {
    
    protected String deploymentIdWithTenant;
    protected String deploymentIdWithoutTenant;
    
    @Before
    public void deployTestCaseModels() {
        this.deploymentIdWithTenant = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-task-model.cmmn").tenantId("test-tenant").deploy().getId();
        this.deploymentIdWithoutTenant = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-task-model.cmmn").deploy().getId();
    }
    
    @After
    public void deleteTestCaseModels() {
        cmmnRepositoryService.deleteDeployment(deploymentIdWithTenant, true);
        cmmnRepositoryService.deleteDeployment(deploymentIdWithoutTenant, true);
    }
    
    @Test
    public void testDeployments() {
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().count());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentWithoutTenantId().count());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentTenantId("test-tenant").count());
    }
    
    @Test
    public void testCaseInstances() {
        for (int i=0; i<3; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        }
        for (int i=0; i<5; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").tenantId("test-tenant").start();
        }
        
        assertEquals(3, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().count());
        assertEquals(5, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("test-tenant").count());
        
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceWithoutTenantId().count());
        assertEquals(5, cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceTenantId("test-tenant").count());
    }

}
