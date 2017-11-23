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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentQueryTest extends FlowableCmmnTestCase {
    
    private String deploymentId1;
    private String deploymentId2;
    
    @Before
    public void deployTestDeployments() {
        this.deploymentId1 = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
            .deploy()
            .getId();
        this.deploymentId2 = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/repository/simple-case2.cmmn")
            .name("testName")
            .category("testCategory")
            .deploy()
            .getId();
    }
    
    @After
    public void deleteTestDeployments() {
        List<CmmnDeployment> deployments = cmmnRepositoryService.createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : deployments) {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        }
    }
    
    @Test
    public void testQueryNoParams() {
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().count());
        
        boolean deployment1Found = false;
        boolean deployment2Found = false;
        for (CmmnDeployment cmmnDeployment : cmmnRepositoryService.createDeploymentQuery().list()) {
            if (deploymentId1.equals(cmmnDeployment.getId())) {
                deployment1Found = true;
            } else if (deploymentId2.equals(cmmnDeployment.getId())) {
                deployment2Found = true;
            }
        }
        assertTrue(deployment1Found);
        assertTrue(deployment2Found);
    }
    
    @Test
    public void testQueryByDeploymentId() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult());
        assertEquals(deploymentId1, cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).count());
        
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).singleResult());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentId() {
        assertNull(cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").singleResult());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentName() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult());
        assertEquals(deploymentId2, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentName() {
        assertNull(cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").singleResult());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentNameLike() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult());
        assertEquals(deploymentId2, cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").count());
        
        assertNull(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").singleResult());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").list().size());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").count());
    }
    
    @Test
    public void testQueryByDeploymentCategory() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult());
        assertEquals(deploymentId2, cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentCategory() {
        assertNull(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").singleResult());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentCategoryNotEquals() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult());
        assertEquals(deploymentId1, cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").count());
    }
    
    @Test
    public void testQueryByDeploymentNameAndCategory() {
        assertNotNull(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult());
        assertEquals(deploymentId2, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult().getId());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").list().size());
        assertEquals(1, cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").count());
    }
    
    @Test
    public void testOrdering() {
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().count());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().count());
        
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymenTime().asc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymenTime().asc().count());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymenTime().desc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymenTime().desc().count());
        
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().count());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().list().size());
        assertEquals(2, cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().count());
    }

}
