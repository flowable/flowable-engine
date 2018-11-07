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
package org.flowable.app.engine.test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class DeploymentQueryTest extends FlowableAppTestCase {
    
    private String deploymentId1;
    private String deploymentId2;
    
    @Before
    public void deployTestDeployments() {
        this.deploymentId1 = appRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/app/engine/test/test.app")
            .deploy()
            .getId();
        this.deploymentId2 = appRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/app/engine/test/fullinfo.app")
            .name("testName")
            .category("testCategory")
            .deploy()
            .getId();
    }
    
    @After
    public void deleteTestDeployments() {
        List<AppDeployment> deployments = appRepositoryService.createDeploymentQuery().list();
        for (AppDeployment appDeployment : deployments) {
            appRepositoryService.deleteDeployment(appDeployment.getId(), true);
        }
    }
    
    @Test
    public void testQueryNoParams() {
        assertEquals(2, appRepositoryService.createDeploymentQuery().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().count());
        
        boolean deployment1Found = false;
        boolean deployment2Found = false;
        for (AppDeployment cmmnDeployment : appRepositoryService.createDeploymentQuery().list()) {
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
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult());
        assertEquals(deploymentId1, appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).count());
        
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).singleResult());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentId() {
        assertNull(appRepositoryService.createDeploymentQuery().deploymentId("invalid").singleResult());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentId("invalid").list().size());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentId("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentName() {
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult());
        assertEquals(deploymentId2, appRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentName("testName").list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentName("testName").count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentName() {
        assertNull(appRepositoryService.createDeploymentQuery().deploymentName("invalid").singleResult());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentName("invalid").list().size());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentName("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentNameLike() {
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult());
        assertEquals(deploymentId2, appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").count());
        
        assertNull(appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").singleResult());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").list().size());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").count());
    }
    
    @Test
    public void testQueryByDeploymentCategory() {
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult());
        assertEquals(deploymentId2, appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").count());
    }
    
    @Test
    public void testQueryByInvalidDeploymentCategory() {
        assertNull(appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").singleResult());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").list().size());
        assertEquals(0, appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").count());
    }
    
    @Test
    public void testQueryByDeploymentCategoryNotEquals() {
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult());
        assertEquals(deploymentId1, appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").count());
    }
    
    @Test
    public void testQueryByDeploymentNameAndCategory() {
        assertNotNull(appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult());
        assertEquals(deploymentId2, appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult().getId());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").list().size());
        assertEquals(1, appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").count());
    }
    
    @Test
    public void testOrdering() {
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().count());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().count());
        
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymenTime().asc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymenTime().asc().count());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymenTime().desc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymenTime().desc().count());
        
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().count());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().list().size());
        assertEquals(2, appRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().count());
    }

}
