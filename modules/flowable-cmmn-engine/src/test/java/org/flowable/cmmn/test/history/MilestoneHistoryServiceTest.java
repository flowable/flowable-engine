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
package org.flowable.cmmn.test.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Dennis Federico
 */
public class MilestoneHistoryServiceTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testMilestoneInstanceHistoryQuery() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMilestoneInstanceHistoryQuery").start();
        //Check setup
        assertCaseInstanceNotEnded(caseInstance);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertEquals(7, planItemInstances.size());
        assertEquals(3, planItemInstances.stream().filter(p -> PlanItemDefinitionType.MILESTONE.equals(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equals(p.getState())).count());
        assertEquals(4, planItemInstances.stream().filter(p -> PlanItemDefinitionType.USER_EVENT_LISTENER.equals(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equals(p.getState())).count());

        List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery().list();
        assertNotNull(historicMilestoneInstances);
        assertTrue(historicMilestoneInstances.isEmpty());

        //event triggering
        setClockTo(new Date(System.currentTimeMillis() + 60_000L));
        Calendar beforeFirstCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        beforeFirstCalendar.add(Calendar.MINUTE, -1);
        Date beforeFirstTrigger = beforeFirstCalendar.getTime();
        PlanItemInstance event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("event1").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        Calendar afterFirstCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        afterFirstCalendar.add(Calendar.MINUTE, 1);
        Date afterFirstTrigger = afterFirstCalendar.getTime();

        setClockTo(new Date(afterFirstTrigger.getTime() + 60_000L));
        Calendar beforeSecondCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        beforeSecondCalendar.add(Calendar.MINUTE, -1);
        Date beforeSecondTrigger = beforeSecondCalendar.getTime();
        event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("event2").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        Calendar afterSecondCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        afterSecondCalendar.add(Calendar.MINUTE, 1);
        Date afterSecondTrigger = afterSecondCalendar.getTime();

        setClockTo(new Date(afterSecondTrigger.getTime() + 60_000L));
        Calendar beforeThirdCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        beforeThirdCalendar.add(Calendar.MINUTE, -1);
        Date beforeThirdTrigger = beforeThirdCalendar.getTime();
        event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("event3").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        Calendar afterThirdCalendar = cmmnEngineConfiguration.getClock().getCurrentCalendar();
        afterThirdCalendar.add(Calendar.MINUTE, 1);
        Date afterThirdTrigger = afterThirdCalendar.getTime();

        //Milestone query is in sync with HistoricMilestone
        assertCaseInstanceNotEnded(caseInstance);
        assertEquals(3, cmmnRuntimeService.createMilestoneInstanceQuery().count());
        assertEquals(1L, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceName("1").milestoneInstanceWithoutTenantId().count());
        assertEquals(1, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceName("1").milestoneInstanceWithoutTenantId().list().size());
        assertEquals(3, cmmnHistoryService.createHistoricMilestoneInstanceQuery().count());

        //Finish Case by triggering the last event
        event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("event4").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());

        //Milestone history is retained after the case ends
        assertCaseInstanceEnded(caseInstance);
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().count());
        assertEquals(3, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(1L, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceName("1").milestoneInstanceWithoutTenantId().count());
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceName("1").milestoneInstanceWithoutTenantId().list().size());

        //There are two named milestones
        HistoricMilestoneInstance abcMilestone = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceName("abcMilestone").singleResult();
        assertNotNull(abcMilestone);
        assertEquals("abcMilestone", abcMilestone.getName());

        HistoricMilestoneInstance xyzMilestone = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceName("xyzMilestone").singleResult();
        assertNotNull(xyzMilestone);
        assertEquals("xyzMilestone", xyzMilestone.getName());

        HistoricMilestoneInstance one = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceName("1").singleResult();
        assertNotNull(one);
        assertEquals("1", one.getName());

        List<HistoricMilestoneInstance> list = cmmnHistoryService.createHistoricMilestoneInstanceQuery().orderByMilestoneName().asc().list();
        assertEquals(3, list.size());
        //assertNull(list.get(0).getName());
        assertEquals("1", list.get(0).getName());
        assertEquals("abcMilestone", list.get(1).getName());
        assertEquals("xyzMilestone", list.get(2).getName());

        //Query timestamps
        HistoricMilestoneInstance milestone1 = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeFirstTrigger)
                .milestoneInstanceReachedBefore(afterFirstTrigger)
                .singleResult();
        assertNotNull(milestone1);
        assertEquals("milestonePlanItem1", milestone1.getElementId());
        HistoricMilestoneInstance milestone2 = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeSecondTrigger)
                .milestoneInstanceReachedBefore(afterSecondTrigger)
                .singleResult();
        assertNotNull(milestone2);
        assertEquals("milestonePlanItem2", milestone2.getElementId());
        HistoricMilestoneInstance milestone3 = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeThirdTrigger)
                .milestoneInstanceReachedBefore(afterThirdTrigger)
                .singleResult();
        assertNotNull(milestone3);
        assertEquals("milestonePlanItem3", milestone3.getElementId());

        list = cmmnHistoryService.createHistoricMilestoneInstanceQuery().orderByTimeStamp().desc().list();
        assertEquals("milestonePlanItem3", list.get(0).getElementId());
        assertEquals("milestonePlanItem2", list.get(1).getElementId());
        assertEquals("milestonePlanItem1", list.get(2).getElementId());
    }
}
