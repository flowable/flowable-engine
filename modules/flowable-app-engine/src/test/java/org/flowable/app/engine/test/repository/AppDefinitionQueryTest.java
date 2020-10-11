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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class AppDefinitionQueryTest extends FlowableAppTestCase {

    private String deploymentId1;
    private String deploymentId2;
    private String deploymentId3;
    private String deploymentId4;

    @Before
    public void deployTestDeployments() {
        // only first app resource is deployed
        this.deploymentId1 = appRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/app/engine/test/test.app")
                .addClasspathResource("org/flowable/app/engine/test/fullinfo.app")
                .deploy()
                .getId();
        
        this.deploymentId2 = appRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/app/engine/test/fullinfo.app")
                .deploy()
                .getId();

        // v2 of test app
        this.deploymentId3 = appRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/app/engine/test/test.app")
                .deploy()
                .getId();

        // v3 of test app
        this.deploymentId4 = appRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/app/engine/test/test.app")
                .deploy()
                .getId();
        
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().list();
        for (AppDefinition appDefinition : appDefinitions) {
            appRepositoryService.setAppDefinitionCategory(appDefinition.getId(), "http://flowable.org/app");
        }
    }

    @After
    public void deleteTestDeployments() {
        List<AppDeployment> deployments = appRepositoryService.createDeploymentQuery().list();
        for (AppDeployment cmmnDeployment : deployments) {
            appRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    @Test
    public void testQueryNoParams() {
        assertThat(appRepositoryService.createAppDefinitionQuery().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testQueryByDeploymentId() {
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId1).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId2).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId2).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId3).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId3).count()).isEqualTo(1);
        
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId4).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId4).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentIds() {
        assertThat(appRepositoryService.createAppDefinitionQuery()
            .deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3, deploymentId4))).list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery()
            .deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3, deploymentId4))).count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).list()).hasSize(2);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).count())
            .isEqualTo(2);

        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentIds() {
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).count()).isZero();
    }

    @Test
    public void testQueryByEmptyDeploymentIds() {
        assertThatThrownBy(() -> appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>()).list())
            .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByAppDefinitionId() {
        List<String> appDefinitionIdsDeployment1 = getAppDefinitionIds(deploymentId1);
        List<String> appDefinitionIdsDeployment2 = getAppDefinitionIds(deploymentId2);
        List<String> appDefinitionIdsDeployment3 = getAppDefinitionIds(deploymentId3);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).singleResult()).isNotNull();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).singleResult()).isNotNull();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).singleResult()).isNotNull();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidAppDefinitionId() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionIds() {
        List<String> appDefinitionIdsDeployment1 = getAppDefinitionIds(deploymentId1);
        List<String> appDefinitionIdsDeployment2 = getAppDefinitionIds(deploymentId2);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment1)).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment1)).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment2)).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment2)).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByEmptyAppDefinitionIds() {
        assertThatThrownBy(() -> appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>()).list())
            .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidAppDefinitionIds() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionCategory() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("http://flowable.org/app").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("http://flowable.org/app").count()).isEqualTo(4);
    }

    @Test
    public void testQueryByInvalidAppDefinitionCategory() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("invalid").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionCategoryLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("http%").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("http%").count()).isEqualTo(4);
    }

    @Test
    public void testQueryByInvalidAppDefinitionCategoryLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("invalid%").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("invalid%n").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionCategoryNotEquals() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("another").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("another").count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("http://flowable.org/app").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("http://flowable.org/app").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionName() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Test app").list()).hasSize(3);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Test app").count()).isEqualTo(3);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").singleResult().getDeploymentId())
            .isEqualTo(deploymentId2);
    }

    @Test
    public void testQueryByInvalidAppDefinitionName() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Case 3").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionName("Case 3").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionNameLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("%app").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("%app").count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("Full%").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("Full%").count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("invalid%").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("invalid%").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionKey() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").list()).hasSize(3);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").count()).isEqualTo(3);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("fullInfoApp").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("fullInfoApp").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidAppDefinitionKey() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("invalid").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("invalid").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionKeyLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%App").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%App").count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("full%").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("full%").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidAppDefinitionKeyLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%invalid").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%invalid").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionVersion() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(1).list()).hasSize(2);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(1).count()).isEqualTo(2);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(4).list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(4).count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionVersionGreaterThan() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(2).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(2).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(3).list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(3).count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionVersionGreaterThanOrEquals() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(2).list()).hasSize(2);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(2).count()).isEqualTo(2);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(3).list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(3).count()).isEqualTo(1);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(4).list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(4).count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionVersionLowerThan() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(2).list()).hasSize(2);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(2).count()).isEqualTo(2);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(3).list()).hasSize(3);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(3).count()).isEqualTo(3);
    }

    @Test
    public void testQueryByAppDefinitionVersionLowerThanOrEquals() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(2).list()).hasSize(3);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(2).count()).isEqualTo(3);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(3).list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(3).count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(4).list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(4).count()).isEqualTo(4);
    }

    @Test
    public void testQueryByLatestVersion() {
        assertThat(appRepositoryService.createAppDefinitionQuery().latestVersion().list()).hasSize(2);
        assertThat(appRepositoryService.createAppDefinitionQuery().latestVersion().count()).isEqualTo(2);
    }

    @Test
    public void testQueryByLatestVersionAndKey() {
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().singleResult();
        assertThat(appDefinition).isNotNull();
        assertThat(appDefinition.getVersion()).isEqualTo(3);
        assertThat(appDefinition.getDeploymentId()).isEqualTo(deploymentId4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().count()).isEqualTo(1);
    }

    @Test
    public void testQueryByAppDefinitionResourceName() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").list()).hasSize(3);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").count()).isEqualTo(3);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/fullinfo.app").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/fullinfo.app").count()).isEqualTo(1);

        assertThat(
            appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").latestVersion().list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").latestVersion().count())
            .isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidAppDefinitionResourceName() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("invalid.app").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("invalid.app").count()).isZero();
    }

    @Test
    public void testQueryByAppDefinitionResourceNameLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%.app").list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%.app").count()).isEqualTo(4);

        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%full%").list()).hasSize(1);
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%full%").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidAppDefinitionResourceNameLike() {
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%invalid%").list()).isEmpty();
        assertThat(appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%invalid%").count()).isZero();
    }

    @Test
    public void testQueryOrderByAppDefinitionCategory() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().asc().count()).isEqualTo(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByAppDefinitionKey() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().count()).isEqualTo(4);
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list();
        assertThat(appDefinitions)
            .extracting(AppDefinition::getKey)
            .containsExactly("fullInfoApp", "testApp", "testApp", "testApp");

        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().count()).isEqualTo(4);
        appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().list();
        assertThat(appDefinitions)
            .extracting(AppDefinition::getKey)
            .containsExactly("testApp", "testApp", "testApp", "fullInfoApp");
    }

    @Test
    public void testQueryOrderByAppDefinitionId() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().asc().count()).isEqualTo(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByAppDefinitionName() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().asc().count()).isEqualTo(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByAppDefinitionDeploymentId() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().asc().count()).isEqualTo(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByAppDefinitionVersion() {
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().asc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().asc().count()).isEqualTo(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().list()).hasSize(4);
        assertThat(appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().count()).isEqualTo(4);

        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().list();
        assertThat(appDefinitions)
            .extracting(AppDefinition::getVersion)
            .containsExactly(3, 2, 1, 1);

        appDefinitions = appRepositoryService.createAppDefinitionQuery().latestVersion().orderByAppDefinitionVersion().asc().list();
        assertThat(appDefinitions)
            .extracting(AppDefinition::getKey, AppDefinition::getVersion)
            .containsExactly(
                tuple("fullInfoApp", 1),
                tuple("testApp", 3)
            );
    }

    private List<String> getAppDefinitionIds(String deploymentId) {
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId).list();
        List<String> ids = new ArrayList<>();
        for (AppDefinition appDefinition : appDefinitions) {
            ids.add(appDefinition.getId());
        }
        return ids;
    }

}
