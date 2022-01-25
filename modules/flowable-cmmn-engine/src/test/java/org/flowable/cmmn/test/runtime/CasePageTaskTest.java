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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.Test;

public class CasePageTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("keyVar", "myFormKeyValue")
                .start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();

        String[] expectedNames = new String[] { "Case Page Task One", "Stage One", "Task One", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Finishing task 2 should complete the stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();

        assertThat(planItemInstances).hasSize(1);
        expectedNames = new String[] { "Task One" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("casePageTask1")
                .planItemInstanceFormKey("myFormKeyValue")
                .includeEnded()
                .singleResult();
        assertThat(pagePlanItemInstance).isNotNull();

        // page tasks go into terminated or completed state, depending on the parent ending type like complete or exit
        assertThat(pagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
        assertThat(pagePlanItemInstance.getFormKey()).isEqualTo("myFormKeyValue");
        assertThat(pagePlanItemInstance.getExtraValue()).isEqualTo("myFormKeyValue");

        pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceFormKey("myFormKeyValue")
                .includeEnded()
                .singleResult();
        assertThat(pagePlanItemInstance).isNotNull();

        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .planItemInstanceFormKey("myFormKeyValue")
                .singleResult();
            assertThat(historicPlanItemInstance).isNotNull();
            assertThat(historicPlanItemInstance.getFormKey()).isEqualTo("myFormKeyValue");
            assertThat(historicPlanItemInstance.getExtraValue()).isEqualTo("myFormKeyValue");
        }
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        UserEventListenerInstance userEventListener = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListener.getId());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();

        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task One");

        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("casePageTask1")
                .includeEnded()
                .singleResult();
        assertThat(pagePlanItemInstance).isNotNull();
        assertThat(pagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testIdentityLinks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("casePageTask1")
                .singleResult();
        assertThat(pagePlanItemInstance).isNotNull();

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForPlanItemInstance(pagePlanItemInstance.getId());

        assertThat(identityLinks)
                .extracting(IdentityLink::getType, IdentityLink::getUserId, IdentityLink::getGroupId)
                .containsExactlyInAnyOrder(
                        tuple(IdentityLinkType.ASSIGNEE, "johndoe", null),
                        tuple(IdentityLinkType.OWNER, "janedoe", null),
                        tuple(IdentityLinkType.CANDIDATE, "johndoe", null),
                        tuple(IdentityLinkType.CANDIDATE, "janedoe", null),
                        tuple(IdentityLinkType.CANDIDATE, null, "sales")
                );

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("janedoe").singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe2").singleResult();
        assertThat(planItemInstance).isNull();

        List<String> groups = Collections.singletonList("sales");
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(groups).singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(groups).singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(groups).singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        List<String> nonMatchingGroups = Collections.singletonList("management");
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(nonMatchingGroups).singleResult();
        assertThat(planItemInstance).isNull();

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(nonMatchingGroups).singleResult();
        assertThat(planItemInstance).isNull();

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(nonMatchingGroups).singleResult();
        assertThat(planItemInstance.getName()).isEqualTo("Case Page Task One");

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).hasSize(4);

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(cmmnEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        Set<String> testGroups = new HashSet<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testGroups.add("group" + i);
        }
        
        PlanItemInstanceQuery planItemInstanceQuery = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(testGroups);
        assertThat(planItemInstanceQuery.count()).isEqualTo(0);
        assertThat(planItemInstanceQuery.list()).hasSize(0);
        
        testGroups.add("sales");
        planItemInstanceQuery = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(testGroups);
        assertThat(planItemInstanceQuery.count()).isEqualTo(1);
        assertThat(planItemInstanceQuery.list()).hasSize(1);
        
        // Finishing task 2 should complete the stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();

        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForPlanItemInstance(pagePlanItemInstance.getId());
            assertThat(historicIdentityLinks)
                    .extracting(HistoricIdentityLink::getType, HistoricIdentityLink::getUserId, HistoricIdentityLink::getGroupId)
                    .containsExactlyInAnyOrder(
                            tuple(IdentityLinkType.ASSIGNEE, "johndoe", null),
                            tuple(IdentityLinkType.OWNER, "janedoe", null),
                            tuple(IdentityLinkType.CANDIDATE, "johndoe", null),
                            tuple(IdentityLinkType.CANDIDATE, "janedoe", null),
                            tuple(IdentityLinkType.CANDIDATE, null, "sales")
                    );

            HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("johndoe").singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("janedoe").singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("johndoe2").singleResult();
            assertThat(historicPlanItemInstance).isNull();

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedGroups(groups).singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(groups).singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(groups).singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedGroups(nonMatchingGroups).singleResult();
            assertThat(historicPlanItemInstance).isNull();

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(nonMatchingGroups).singleResult();
            assertThat(historicPlanItemInstance).isNull();

            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(nonMatchingGroups).singleResult();
            assertThat(historicPlanItemInstance.getName()).isEqualTo("Case Page Task One");

            testGroups.remove("sales");
            HistoricPlanItemInstanceQuery historicPlanItemInstanceQuery = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedGroups(testGroups);
            assertThat(historicPlanItemInstanceQuery.count()).isEqualTo(0);
            assertThat(historicPlanItemInstanceQuery.list()).hasSize(0);
            
            testGroups.add("sales");
            historicPlanItemInstanceQuery = cmmnHistoryService.createHistoricPlanItemInstanceQuery().involvedGroups(testGroups);
            assertThat(historicPlanItemInstanceQuery.count()).isEqualTo(1);
            assertThat(historicPlanItemInstanceQuery.list()).hasSize(1);
        }
    }
}
