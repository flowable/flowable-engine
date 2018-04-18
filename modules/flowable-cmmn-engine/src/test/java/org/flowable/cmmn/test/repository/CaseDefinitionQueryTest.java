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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CaseDefinitionQueryTest extends FlowableCmmnTestCase {

    private String deploymentId1;
    private String deploymentId2;
    private String deploymentId3;

    @Before
    public void deployTestDeployments() {
        this.deploymentId1 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case2.cmmn")
                .deploy()
                .getId();

        // v2 of simple-case
        this.deploymentId2 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .deploy()
                .getId();

        // v3 of simple-case
        this.deploymentId3 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
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
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().count());
    }

    @Test
    public void testQueryByDeploymentId() {
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId2).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId2).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId3).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId3).count());
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().deploymentId("invalid").count());
    }

    @Test
    public void testQueryByDeploymentIds() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3))).list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3))).count());

        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).count());

        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).count());
    }

    @Test
    public void testQueryByInvalidDeploymentIds() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).count());
    }

    @Test
    public void testQueryByEmptyDeploymentIds() {
        try {
            cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<String>()).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByCaseDefinitionId() {
        List<String> caseDefinitionIdsDeployment1 = getCaseDefinitionIds(deploymentId1);
        List<String> caseDefinitionIdsDeployment2 = getCaseDefinitionIds(deploymentId2);
        List<String> caseDefinitionIdsDeployment3 = getCaseDefinitionIds(deploymentId3);

        assertNotNull(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).singleResult());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).count());

        assertNotNull(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).singleResult());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).count());

        assertNotNull(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).singleResult());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).count());

        assertNotNull(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).singleResult());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionId() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId("invalid").count());
    }

    @Test
    public void testQueryByCaseDefinitionIds() {
        List<String> caseDefinitionIdsDeployment1 = getCaseDefinitionIds(deploymentId1);
        List<String> caseDefinitionIdsDeployment2 = getCaseDefinitionIds(deploymentId2);

        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment1)).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment1)).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment2)).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment2)).count());
    }

    @Test
    public void testQueryByEmptyCaseDefinitionIds() {
        try {
            cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<String>()).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByInvalidCaseDefinitionIds() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).count());
    }

    @Test
    public void testQueryByCaseDefinitionCategory() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionCategory() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("invalid").count());
    }

    @Test
    public void testQueryByCaseDefinitionCategoryLike() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("http%").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("http%n").count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionCategoryLike() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("invalid%").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("invalid%n").count());
    }

    @Test
    public void testQueryByCaseDefinitionCategoryNotEquals() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("another").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("another").count());

        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("http://flowable.org/cmmn").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("http://flowable.org/cmmn").count());
    }

    @Test
    public void testQueryByCaseDefinitionName() {
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 1").list().size());
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 1").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").count());

        assertEquals(deploymentId1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").singleResult().getDeploymentId());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionName() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 3").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 3").count());
    }

    @Test
    public void testQueryByCaseDefinitionNameLike() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("Ca%").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("Ca%").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("%2").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("%2").count());

        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("invalid%").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("invalid%").count());
    }

    @Test
    public void testQueryByCaseDefinitionKey() {
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").list().size());
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase2").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase2").count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionKey() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("invalid").count());
    }

    @Test
    public void testQueryByCaseDefinitionKeyLike() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("my%").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("my%").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%2").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%2").count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionKeyLike() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%invalid").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%invalid").count());
    }

    @Test
    public void testQueryByCaseDefinitionVersion() {
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(1).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(1).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).count());

        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(4).list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(4).count());
    }

    @Test
    public void testQueryByCaseDefinitionVersionGreaterThan() {
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(2).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(2).count());

        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(3).list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(3).count());
    }

    @Test
    public void testQueryByCaseDefinitionVersionGreaterThanOrEquals() {
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(2).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(2).count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(3).list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(3).count());

        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(4).list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(4).count());
    }

    @Test
    public void testQueryByCaseDefinitionVersionLowerThan() {
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(2).list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(2).count());

        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(3).list().size());
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(3).count());
    }

    @Test
    public void testQueryByCaseDefinitionVersionLowerThanOrEquals() {
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(2).list().size());
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(2).count());

        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(3).list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(3).count());

        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(4).list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(4).count());
    }

    @Test
    public void testQueryByLatestVersion() {
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().list().size());
        assertEquals(2, cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().count());
    }

    @Test
    public void testQueryByLatestVersionAndKey() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().singleResult();
        assertNotNull(caseDefinition);
        assertEquals(3, caseDefinition.getVersion());
        assertEquals(deploymentId3, caseDefinition.getDeploymentId());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().count());
    }

    @Test
    public void testQueryByCaseDefinitionResourceName() {
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").list().size());
        assertEquals(3, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case2.cmmn").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case2.cmmn").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").latestVersion().list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").latestVersion().count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionResourceName() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("invalid.cmmn").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("invalid.cmmn").count());
    }

    @Test
    public void testQueryByCaseDefinitionResourceNameLike() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%.cmmn").list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%.cmmn").count());

        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%2%").list().size());
        assertEquals(1, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%2%").count());
    }

    @Test
    public void testQueryByInvalidCaseDefinitionResourceNameLike() {
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%invalid%").list().size());
        assertEquals(0, cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%invalid%").count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionCategory() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().asc().count());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().desc().count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionKey() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().count());
        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        for (int i = 0; i < caseDefinitions.size(); i++) {
            if (i <= 2) {
                assertEquals("myCase", caseDefinitions.get(i).getKey());
            } else {
                assertEquals("myCase2", caseDefinitions.get(i).getKey());
            }
        }

        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().count());
        caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().list();
        for (int i = 0; i < caseDefinitions.size(); i++) {
            if (i > 0) {
                assertEquals("myCase", caseDefinitions.get(i).getKey());
            } else {
                assertEquals("myCase2", caseDefinitions.get(i).getKey());
            }
        }
    }

    @Test
    public void testQueryOrderByCaseDefinitionId() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().asc().count());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().desc().count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionName() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().count());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().desc().count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionDeploymentId() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().asc().count());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().desc().count());
    }

    @Test
    public void testQueryOrderByCaseDefinitionVersion() {
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().asc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().asc().count());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().list().size());
        assertEquals(4, cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().count());

        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().list();
        assertEquals(3, caseDefinitions.get(0).getVersion());
        assertEquals(2, caseDefinitions.get(1).getVersion());
        assertEquals(1, caseDefinitions.get(2).getVersion());
        assertEquals(1, caseDefinitions.get(3).getVersion());

        caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().orderByCaseDefinitionVersion().asc().list();
        assertEquals(1, caseDefinitions.get(0).getVersion());
        assertEquals("myCase2", caseDefinitions.get(0).getKey());
        assertEquals(3, caseDefinitions.get(1).getVersion());
        assertEquals("myCase", caseDefinitions.get(1).getKey());
    }

    private List<String> getCaseDefinitionIds(String deploymentId) {
        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId).list();
        List<String> ids = new ArrayList<>();
        for (CaseDefinition caseDefinition : caseDefinitions) {
            ids.add(caseDefinition.getId());
        }
        return ids;
    }

}
