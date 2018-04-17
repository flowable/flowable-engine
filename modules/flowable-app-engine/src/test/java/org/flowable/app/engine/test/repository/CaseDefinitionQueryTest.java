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
import static org.junit.Assert.fail;

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
public class CaseDefinitionQueryTest extends FlowableAppTestCase {

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
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().count());
    }

    @Test
    public void testQueryByDeploymentId() {
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId1).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId1).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId2).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId2).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId3).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId3).count());
        
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId4).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentId(deploymentId4).count());
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().deploymentId("invalid").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().deploymentId("invalid").count());
    }

    @Test
    public void testQueryByDeploymentIds() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3, deploymentId4))).list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3, deploymentId4))).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).count());

        assertEquals(2, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).list().size());
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).count());
    }

    @Test
    public void testQueryByInvalidDeploymentIds() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).count());
    }

    @Test
    public void testQueryByEmptyDeploymentIds() {
        try {
            appRepositoryService.createAppDefinitionQuery().deploymentIds(new HashSet<String>()).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByAppDefinitionId() {
        List<String> appDefinitionIdsDeployment1 = getAppDefinitionIds(deploymentId1);
        List<String> appDefinitionIdsDeployment2 = getAppDefinitionIds(deploymentId2);
        List<String> appDefinitionIdsDeployment3 = getAppDefinitionIds(deploymentId3);

        assertNotNull(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).singleResult());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment1.get(0)).count());

        assertNotNull(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).singleResult());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment2.get(0)).count());

        assertNotNull(appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).singleResult());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionId(appDefinitionIdsDeployment3.get(0)).count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionId() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionId("invalid").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionId("invalid").count());
    }

    @Test
    public void testQueryByAppDefinitionIds() {
        List<String> appDefinitionIdsDeployment1 = getAppDefinitionIds(deploymentId1);
        List<String> appDefinitionIdsDeployment2 = getAppDefinitionIds(deploymentId2);

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment1)).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment1)).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment2)).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(appDefinitionIdsDeployment2)).count());
    }

    @Test
    public void testQueryByEmptyAppDefinitionIds() {
        try {
            appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<String>()).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByInvalidAppDefinitionIds() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).count());
    }

    @Test
    public void testQueryByAppDefinitionCategory() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("http://flowable.org/app").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("http://flowable.org/app").count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionCategory() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("invalid").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategory("invalid").count());
    }

    @Test
    public void testQueryByAppDefinitionCategoryLike() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("http%").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("http%").count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionCategoryLike() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("invalid%").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryLike("invalid%n").count());
    }

    @Test
    public void testQueryByAppDefinitionCategoryNotEquals() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("another").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("another").count());

        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("http://flowable.org/app").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionCategoryNotEquals("http://flowable.org/app").count());
    }

    @Test
    public void testQueryByAppDefinitionName() {
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Test app").list().size());
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Test app").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").count());

        assertEquals(deploymentId2, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Full info app").singleResult().getDeploymentId());
    }

    @Test
    public void testQueryByInvalidAppDefinitionName() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Case 3").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionName("Case 3").count());
    }

    @Test
    public void testQueryByAppDefinitionNameLike() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("%app").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("%app").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("Full%").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("Full%").count());

        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("invalid%").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionNameLike("invalid%").count());
    }

    @Test
    public void testQueryByAppDefinitionKey() {
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").list().size());
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("fullInfoApp").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("fullInfoApp").count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionKey() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("invalid").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("invalid").count());
    }

    @Test
    public void testQueryByAppDefinitionKeyLike() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%App").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%App").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("full%").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("full%").count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionKeyLike() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%invalid").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionKeyLike("%invalid").count());
    }

    @Test
    public void testQueryByAppDefinitionVersion() {
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(1).list().size());
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(1).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(2).count());

        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(4).list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersion(4).count());
    }

    @Test
    public void testQueryByAppDefinitionVersionGreaterThan() {
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(2).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(2).count());

        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(3).list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThan(3).count());
    }

    @Test
    public void testQueryByAppDefinitionVersionGreaterThanOrEquals() {
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(2).list().size());
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(2).count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(3).list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(3).count());

        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(4).list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionGreaterThanOrEquals(4).count());
    }

    @Test
    public void testQueryByAppDefinitionVersionLowerThan() {
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(2).list().size());
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(2).count());

        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(3).list().size());
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThan(3).count());
    }

    @Test
    public void testQueryByAppDefinitionVersionLowerThanOrEquals() {
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(2).list().size());
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(2).count());

        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(3).list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(3).count());

        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(4).list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionVersionLowerThanOrEquals(4).count());
    }

    @Test
    public void testQueryByLatestVersion() {
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().latestVersion().list().size());
        assertEquals(2, appRepositoryService.createAppDefinitionQuery().latestVersion().count());
    }

    @Test
    public void testQueryByLatestVersionAndKey() {
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().singleResult();
        assertNotNull(appDefinition);
        assertEquals(3, appDefinition.getVersion());
        assertEquals(deploymentId4, appDefinition.getDeploymentId());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionKey("testApp").latestVersion().count());
    }

    @Test
    public void testQueryByAppDefinitionResourceName() {
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").list().size());
        assertEquals(3, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/fullinfo.app").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/fullinfo.app").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").latestVersion().list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("org/flowable/app/engine/test/test.app").latestVersion().count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionResourceName() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("invalid.app").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceName("invalid.app").count());
    }

    @Test
    public void testQueryByAppDefinitionResourceNameLike() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%.app").list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%.app").count());

        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%full%").list().size());
        assertEquals(1, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%full%").count());
    }

    @Test
    public void testQueryByInvalidAppDefinitionResourceNameLike() {
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%invalid%").list().size());
        assertEquals(0, appRepositoryService.createAppDefinitionQuery().appDefinitionResourceNameLike("%invalid%").count());
    }

    @Test
    public void testQueryOrderByAppDefinitionCategory() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().asc().count());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionCategory().desc().count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionKey() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().count());
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list();
        for (int i = 0; i < appDefinitions.size(); i++) {
            if (i > 0) {
                assertEquals("testApp", appDefinitions.get(i).getKey());
            } else {
                assertEquals("fullInfoApp", appDefinitions.get(i).getKey());
            }
        }

        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().count());
        appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().desc().list();
        for (int i = 0; i < appDefinitions.size(); i++) {
            if (i <= 2) {
                assertEquals("testApp", appDefinitions.get(i).getKey());
            } else {
                assertEquals("fullInfoApp", appDefinitions.get(i).getKey());
            }
        }
    }

    @Test
    public void testQueryOrderByAppDefinitionId() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().asc().count());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionId().desc().count());
    }

    @Test
    public void testQueryOrderByAppDefinitionName() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().asc().count());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionName().desc().count());
    }

    @Test
    public void testQueryOrderByAppDefinitionDeploymentId() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().asc().count());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByDeploymentId().desc().count());
    }

    @Test
    public void testQueryOrderByAppDefinitionVersion() {
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().asc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().asc().count());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().list().size());
        assertEquals(4, appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().count());

        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().orderByAppDefinitionVersion().desc().list();
        assertEquals(3, appDefinitions.get(0).getVersion());
        assertEquals(2, appDefinitions.get(1).getVersion());
        assertEquals(1, appDefinitions.get(2).getVersion());
        assertEquals(1, appDefinitions.get(3).getVersion());

        appDefinitions = appRepositoryService.createAppDefinitionQuery().latestVersion().orderByAppDefinitionVersion().asc().list();
        assertEquals(1, appDefinitions.get(0).getVersion());
        assertEquals("fullInfoApp", appDefinitions.get(0).getKey());
        assertEquals(3, appDefinitions.get(1).getVersion());
        assertEquals("testApp", appDefinitions.get(1).getKey());
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
