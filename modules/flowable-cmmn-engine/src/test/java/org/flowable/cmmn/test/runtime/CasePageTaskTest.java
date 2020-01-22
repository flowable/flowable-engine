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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
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
        assertEquals(4, planItemInstances.size());
        String[] expectedNames = new String[] { "Case Page Task One", "Stage One", "Task One", "Task Two"};
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
       
        // Finishing task 2 should complete the stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
       
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                        .orderByName().asc()
                        .list();
        
        assertEquals(1, planItemInstances.size());
        expectedNames = new String[] { "Task One" };
        for (int i=0; i<planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
        }
       
        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId("casePageTask1")
                        .planItemInstanceFormKey("myFormKeyValue")
                        .includeEnded()
                        .singleResult();
        assertNotNull(pagePlanItemInstance);

        // page tasks go into terminated state, if their parent stage gets completed, regardless its previous state
        assertEquals(PlanItemInstanceState.TERMINATED, pagePlanItemInstance.getState());
        assertEquals("myFormKeyValue", pagePlanItemInstance.getFormKey());
        assertEquals("myFormKeyValue", pagePlanItemInstance.getExtraValue());
        
        pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemInstanceFormKey("myFormKeyValue")
                        .includeEnded()
                        .singleResult();
        assertNotNull(pagePlanItemInstance);
       
        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        
        HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .planItemInstanceFormKey("myFormKeyValue")
                .singleResult();
        assertNotNull(historicPlanItemInstance);
        assertEquals("myFormKeyValue", historicPlanItemInstance.getFormKey());
        assertEquals("myFormKeyValue", historicPlanItemInstance.getExtraValue());
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        UserEventListenerInstance userEventListener = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
               .caseInstanceId(caseInstance.getId())
               .planItemInstanceState(PlanItemInstanceState.ACTIVE)
               .orderByName().asc()
               .list();
       
        assertEquals(1, planItemInstances.size());
        assertEquals("Task One", planItemInstances.get(0).getName());
       
        PlanItemInstance pagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId("casePageTask1")
                        .includeEnded()
                        .singleResult();
        assertNotNull(pagePlanItemInstance);
        assertEquals(PlanItemInstanceState.TERMINATED, pagePlanItemInstance.getState());
       
        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
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
        assertNotNull(pagePlanItemInstance);
        
        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForPlanItemInstance(pagePlanItemInstance.getId());
        assertEquals(5, identityLinks.size());
        
        List<IdentityLink> assigneeLink = identityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.ASSIGNEE)).collect(Collectors.toList());
        assertEquals(1, assigneeLink.size());
        assertEquals("johndoe", assigneeLink.get(0).getUserId());
        
        List<IdentityLink> ownerLink = identityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.OWNER)).collect(Collectors.toList());
        assertEquals(1, ownerLink.size());
        assertEquals("janedoe", ownerLink.get(0).getUserId());
        
        List<IdentityLink> candidateUserLinks = identityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) && 
                        identityLink.getUserId() != null).collect(Collectors.toList());
        assertEquals(2, candidateUserLinks.size());
        List<String> linkValues = new ArrayList<>();
        for (IdentityLink candidateLink : candidateUserLinks) {
            linkValues.add(candidateLink.getUserId());
        }
        assertTrue(linkValues.contains("johndoe"));
        assertTrue(linkValues.contains("janedoe"));
        
        List<IdentityLink> groupLink = identityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) &&
                        identityLink.getGroupId() != null).collect(Collectors.toList());
        assertEquals(1, groupLink.size());
        assertEquals("sales", groupLink.get(0).getGroupId());
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("janedoe").singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe2").singleResult();
        assertNull(planItemInstance);
        
        List<String> groups = new ArrayList<>();
        groups.add("sales");
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(groups).singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(groups).singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(groups).singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        List<String> nonMatchingGroups = new ArrayList<>();
        nonMatchingGroups.add("management");
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedGroups(nonMatchingGroups).singleResult();
        assertNull(planItemInstance);
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("nonexisting").involvedGroups(nonMatchingGroups).singleResult();
        assertNull(planItemInstance);
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().involvedUser("johndoe").involvedGroups(nonMatchingGroups).singleResult();
        assertEquals("Case Page Task One", planItemInstance.getName());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                        .orderByName().asc()
                        .list();
        assertEquals(4, planItemInstances.size());
       
        // Finishing task 2 should complete the stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                        .orderByName().asc()
                        .list();
       
        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        
        List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForPlanItemInstance(pagePlanItemInstance.getId());
        assertEquals(5, historicIdentityLinks.size());
        
        List<HistoricIdentityLink> historicAssigneeLink = historicIdentityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.ASSIGNEE)).collect(Collectors.toList());
        assertEquals(1, historicAssigneeLink.size());
        assertEquals("johndoe", historicAssigneeLink.get(0).getUserId());
        
        List<HistoricIdentityLink> historicOwnerLink = historicIdentityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.OWNER)).collect(Collectors.toList());
        assertEquals(1, historicOwnerLink.size());
        assertEquals("janedoe", historicOwnerLink.get(0).getUserId());
        
        List<HistoricIdentityLink> historicCandidateUserLinks = historicIdentityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) && 
                        identityLink.getUserId() != null).collect(Collectors.toList());
        assertEquals(2, historicCandidateUserLinks.size());
        linkValues = new ArrayList<>();
        for (HistoricIdentityLink candidateLink : historicCandidateUserLinks) {
            linkValues.add(candidateLink.getUserId());
        }
        assertTrue(linkValues.contains("johndoe"));
        assertTrue(linkValues.contains("janedoe"));
        
        List<HistoricIdentityLink> historicGroupLink = historicIdentityLinks.stream().filter(identityLink -> identityLink.getType().equals(IdentityLinkType.CANDIDATE) &&
                        identityLink.getGroupId() != null).collect(Collectors.toList());
        assertEquals(1, historicGroupLink.size());
        assertEquals("sales", historicGroupLink.get(0).getGroupId());
    }
}
