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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CaseDefinitionLocalizationManager;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
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
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testQueryByDeploymentId() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId2).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId2).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId3).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId3).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByDeploymentIds() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3)))
                .list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId1, deploymentId2, deploymentId3)))
                .count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId1))).count())
                .isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).list())
                .hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Arrays.asList(deploymentId2, deploymentId3))).count())
                .isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList(deploymentId3))).count())
                .isEqualTo(1);
    }

    @Test
    public void testQueryByParentDeploymentId() {
        CmmnDeployment deployment1 = cmmnRepositoryService.createDeployment()
                .parentDeploymentId("parent1")
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .deploy();

        CmmnDeployment deployment2 = cmmnRepositoryService.createDeployment()
                .parentDeploymentId("parent2")
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case2.cmmn")
                .deploy();

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("parent1").list())
                .extracting(CaseDefinition::getKey, CaseDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("myCase", deployment1.getId())
                );
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("parent1").count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("parent2").list())
                .extracting(CaseDefinition::getKey, CaseDefinition::getDeploymentId)
                .containsExactlyInAnyOrder(
                        tuple("myCase", deployment2.getId()),
                        tuple("myCase2", deployment2.getId())
                );
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("parent2").count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("unknown").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().parentDeploymentId("unknown").count()).isEqualTo(0);
    }

    @Test
    public void testQueryByInvalidDeploymentIds() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>(Collections.singletonList("invalid"))).count()).isZero();
    }

    @Test
    public void testQueryByEmptyDeploymentIds() {
        assertThatThrownBy(() -> cmmnRepositoryService.createCaseDefinitionQuery().deploymentIds(new HashSet<>()).list())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByCaseDefinitionId() {
        List<String> caseDefinitionIdsDeployment1 = getCaseDefinitionIds(deploymentId1);
        List<String> caseDefinitionIdsDeployment2 = getCaseDefinitionIds(deploymentId2);
        List<String> caseDefinitionIdsDeployment3 = getCaseDefinitionIds(deploymentId3);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(0)).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment1.get(1)).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment2.get(0)).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).singleResult()).isNotNull();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId(caseDefinitionIdsDeployment3.get(0)).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionId() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionIds() {
        List<String> caseDefinitionIdsDeployment1 = getCaseDefinitionIds(deploymentId1);
        List<String> caseDefinitionIdsDeployment2 = getCaseDefinitionIds(deploymentId2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment1)).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment1)).count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment2)).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(caseDefinitionIdsDeployment2)).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByEmptyCaseDefinitionIds() {
        assertThatThrownBy(() -> cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>()).list())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionIds() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionIds(new HashSet<>(Arrays.asList("invalid1", "invalid2"))).count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionCategory() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").count()).isEqualTo(4);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionCategory() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategory("invalid").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionCategoryLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("http%").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("http%n").count()).isEqualTo(4);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionCategoryLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("invalid%").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryLike("invalid%n").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionCategoryNotEquals() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("another").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("another").count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("http://flowable.org/cmmn").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionCategoryNotEquals("http://flowable.org/cmmn").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionName() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 1").list()).hasSize(3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 1").count()).isEqualTo(3);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 2").singleResult().getDeploymentId()).isEqualTo(deploymentId1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionName() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 3").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionName("Case 3").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionNameLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("Ca%").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("Ca%").count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("%2").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("%2").count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("invalid%").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLike("invalid%").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionNameLikeIgnoreCase() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("ca%").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("ca%").count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("%A%2").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("%A%2").count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("invalid%").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase("invalid%").count()).isZero();

        assertThatThrownBy(() -> cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionNameLikeIgnoreCase(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("nameLikeIgnoreCase is null");
    }

    @Test
    public void testQueryByCaseDefinitionKey() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").list()).hasSize(3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").count()).isEqualTo(3);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase2").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase2").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionKey() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("invalid").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionKeyLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("my%").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("my%").count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%2").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%2").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionKeyLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%invalid").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKeyLike("%invalid").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionVersion() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(1).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(1).count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(2).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(4).list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersion(4).count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionVersionGreaterThan() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(2).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(2).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(3).list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThan(3).count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionVersionGreaterThanOrEquals() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(2).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(2).count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(3).list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(3).count()).isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(4).list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionGreaterThanOrEquals(4).count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionVersionLowerThan() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(2).list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(2).count()).isEqualTo(2);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(3).list()).hasSize(3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThan(3).count()).isEqualTo(3);
    }

    @Test
    public void testQueryByCaseDefinitionVersionLowerThanOrEquals() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(2).list()).hasSize(3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(2).count()).isEqualTo(3);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(3).list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(3).count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(4).list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionVersionLowerThanOrEquals(4).count()).isEqualTo(4);
    }

    @Test
    public void testQueryByLatestVersion() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().list()).hasSize(2);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().count()).isEqualTo(2);
    }

    @Test
    public void testQueryByLatestVersionAndKey() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().singleResult();
        assertThat(caseDefinition).isNotNull();
        assertThat(caseDefinition.getVersion()).isEqualTo(3);
        assertThat(caseDefinition.getDeploymentId()).isEqualTo(deploymentId3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().count()).isEqualTo(1);
    }

    @Test
    public void testQueryByCaseDefinitionResourceName() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").list()
        ).hasSize(3);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn").count())
                .isEqualTo(3);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case2.cmmn").list()
        ).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case2.cmmn").count())
                .isEqualTo(1);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .latestVersion().list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .latestVersion().count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionResourceName() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("invalid.cmmn").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("invalid.cmmn").count()).isZero();
    }

    @Test
    public void testQueryByCaseDefinitionResourceNameLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%.cmmn").list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%.cmmn").count()).isEqualTo(4);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%2%").list()).hasSize(1);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%2%").count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCaseDefinitionResourceNameLike() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%invalid%").list()).isEmpty();
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionResourceNameLike("%invalid%").count()).isZero();
    }

    @Test
    public void testQueryOrderByCaseDefinitionCategory() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().asc().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionCategory().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByCaseDefinitionKey() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().count()).isEqualTo(4);
        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();
        for (int i = 0; i < caseDefinitions.size(); i++) {
            if (i <= 2) {
                assertThat(caseDefinitions.get(i).getKey()).isEqualTo("myCase");
            } else {
                assertThat(caseDefinitions.get(i).getKey()).isEqualTo("myCase2");
            }
        }

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().count()).isEqualTo(4);
        caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().desc().list();
        for (int i = 0; i < caseDefinitions.size(); i++) {
            if (i > 0) {
                assertThat(caseDefinitions.get(i).getKey()).isEqualTo("myCase");
            } else {
                assertThat(caseDefinitions.get(i).getKey()).isEqualTo("myCase2");
            }
        }
    }

    @Test
    public void testQueryOrderByCaseDefinitionId() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().asc().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionId().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByCaseDefinitionName() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByCaseDefinitionDeploymentId() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().asc().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByDeploymentId().desc().count()).isEqualTo(4);
    }

    @Test
    public void testQueryOrderByCaseDefinitionVersion() {
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().asc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().asc().count()).isEqualTo(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().list()).hasSize(4);
        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().count()).isEqualTo(4);

        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionVersion().desc().list();
        assertThat(caseDefinitions.get(0).getVersion()).isEqualTo(3);
        assertThat(caseDefinitions.get(1).getVersion()).isEqualTo(2);
        assertThat(caseDefinitions.get(2).getVersion()).isEqualTo(1);
        assertThat(caseDefinitions.get(3).getVersion()).isEqualTo(1);

        caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().latestVersion().orderByCaseDefinitionVersion().asc().list();
        assertThat(caseDefinitions)
                .extracting(CaseDefinition::getVersion, CaseDefinition::getKey)
                .containsExactly(
                        tuple(1, "myCase2"),
                        tuple(3, "myCase"));
        assertThat(caseDefinitions.get(0).getVersion()).isEqualTo(1);
        assertThat(caseDefinitions.get(0).getKey()).isEqualTo("myCase2");
        assertThat(caseDefinitions.get(1).getVersion()).isEqualTo(3);
        assertThat(caseDefinitions.get(1).getKey()).isEqualTo("myCase");
    }

    @Test
    public void testLocalization() {
        cmmnEngineConfiguration.setCaseDefinitionLocalizationManager(new CaseDefinitionLocalizationManager() {
            @Override
            public void localize(CaseDefinition caseDefinition, String locale, boolean withLocalizationFallback) {
                if ("pt".equals(locale)) {
                    caseDefinition.setLocalizedName("Caso 1");
                    caseDefinition.setLocalizedDescription("Isto é o exemplo de uma descrição");
                }
            }
        });

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().singleResult();
        assertThat(caseDefinition.getName()).isEqualTo("Case 1");
        assertThat(caseDefinition.getDescription()).isEqualTo("This is a sample description");

        caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").latestVersion().locale("pt").singleResult();
        assertThat(caseDefinition.getName()).isEqualTo("Caso 1");
        assertThat(caseDefinition.getDescription()).isEqualTo("Isto é o exemplo de uma descrição");
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
