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

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(appRepositoryService.createDeploymentQuery().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().count()).isEqualTo(2);

        assertThat(appRepositoryService.createDeploymentQuery().list())
            .extracting(AppDeployment::getId)
            .containsExactlyInAnyOrder(deploymentId1, deploymentId2);
    }
    
    @Test
    public void testQueryByDeploymentId() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult().getId()).isEqualTo(deploymentId1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).count()).isEqualTo(1);
        
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).count()).isEqualTo(1);
    }
    
    @Test
    public void testQueryByInvalidDeploymentId() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentId("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDeploymentName() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").count()).isEqualTo(1);
    }
    
    @Test
    public void testQueryByInvalidDeploymentName() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("invalid").singleResult()).isNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDeploymentNameLike() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("test%").count()).isEqualTo(1);
        
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").singleResult()).isNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").list()).isEmpty();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").count()).isZero();
    }
    
    @Test
    public void testQueryByDeploymentCategory() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").count()).isEqualTo(1);
    }
    
    @Test
    public void testQueryByInvalidDeploymentCategory() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").singleResult()).isNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategory("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByDeploymentCategoryNotEquals() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult().getId()).isEqualTo(deploymentId1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").count()).isEqualTo(1);
    }
    
    @Test
    public void testQueryByDeploymentNameAndCategory() {
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult()).isNotNull();
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult().getId())
            .isEqualTo(deploymentId2);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").list()).hasSize(1);
        assertThat(appRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").count()).isEqualTo(1);
    }
    
    @Test
    public void testOrdering() {
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().count()).isEqualTo(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().count()).isEqualTo(2);
        
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentTime().asc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentTime().asc().count()).isEqualTo(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentTime().desc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentTime().desc().count()).isEqualTo(2);
        
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().count()).isEqualTo(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().list()).hasSize(2);
        assertThat(appRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().count()).isEqualTo(2);
    }

}
