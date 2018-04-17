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
package org.flowable.cmmn.test.task;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskPostProcessor;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.junit.After;
import org.junit.Test;

import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Joram Barrez
 */
public class CmmnTaskServiceTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testOneHumanTaskCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task", task.getName());
        assertEquals("This is a test documentation", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertNull(historicTaskInstance.getEndTime());
        }

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task", historicTaskInstance.getName());
            assertEquals("This is a test documentation", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }

    @Test
    @CmmnDeployment
    public void testOneHumanTaskExpressionCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("oneHumanTaskCase")
                        .variable("var1", "A")
                        .variable("var2", "YES")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("The Task A", task.getName());
        assertEquals("This is a test YES", task.getDescription());
        assertEquals("johnDoe", task.getAssignee());
        
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
        
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertEquals("The Task A", historicTaskInstance.getName());
            assertEquals("This is a test YES", historicTaskInstance.getDescription());
            assertNotNull(historicTaskInstance.getEndTime());
        }
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerOneHumanTaskCaseProgrammatically() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
        assertEquals(planItemInstance.getId(), task.getSubScopeId());
        assertEquals(planItemInstance.getCaseInstanceId(), task.getScopeId());
        assertEquals(planItemInstance.getCaseDefinitionId(), task.getScopeDefinitionId());
        assertEquals(ScopeTypes.CMMN, task.getScopeType());
        
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().count());
        assertCaseInstanceEnded(caseInstance);
    }

    private static Set<IdentityLinkEntityImpl> getDefaultIdentityLinks() {
        IdentityLinkEntityImpl identityLinkEntityCandidateUser = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateUser.setUserId("testUserFromBuilder");
        identityLinkEntityCandidateUser.setType(IdentityLinkType.CANDIDATE);
        IdentityLinkEntityImpl identityLinkEntityCandidateGroup = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateGroup.setGroupId("testGroupFromBuilder");
        identityLinkEntityCandidateGroup.setType(IdentityLinkType.CANDIDATE);

        return Stream.of(
                identityLinkEntityCandidateUser,
                identityLinkEntityCandidateGroup
        ).collect(toSet());
    }

}
