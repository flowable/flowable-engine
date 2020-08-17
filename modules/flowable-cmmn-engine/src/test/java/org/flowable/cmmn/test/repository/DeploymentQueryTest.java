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
        assertThat(cmmnRepositoryService.createDeploymentQuery().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createDeploymentQuery().list())
                .extracting(CmmnDeployment::getId)
                .contains(deploymentId1, deploymentId2);
    }

    @Test
    public void testQueryByDeploymentId() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult().getId()).isEqualTo(deploymentId1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId1).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId2).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentName() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentName() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").singleResult()).isNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("invalid").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentNameLike() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("test%").count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").singleResult()).isNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").list()).isEmpty();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentNameLike("inval%").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentCategory() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").singleResult().getId()).isEqualTo(deploymentId2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("testCategory").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentCategory() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").singleResult()).isNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategory("invalid").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentCategoryNotEquals() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").singleResult().getId()).isEqualTo(deploymentId1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategory").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByDeploymentNameAndCategory() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").singleResult().getId())
                .isEqualTo(deploymentId2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createDeploymentQuery().deploymentName("testName").deploymentCategory("testCategory").count()).isEqualTo(1);
    }

    @Test
    public void testOrdering() {
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().asc().count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentId().desc().count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentTime().asc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentTime().asc().count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentTime().desc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentTime().desc().count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().asc().count()).isEqualTo(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createDeploymentQuery().orderByDeploymentName().desc().count()).isEqualTo(2);
    }

}
