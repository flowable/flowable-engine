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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author Dennis Federico
 */
public class MilestoneQueryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleMilestoneInstanceQuery() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleMilestoneInstanceQuery").start();
        //Check setup
        assertCaseInstanceNotEnded(caseInstance);
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertThat(planItemInstances).hasSize(7);
        assertThat(planItemInstances.stream().filter(p -> PlanItemDefinitionType.MILESTONE.equals(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equals(p.getState())).count()).isEqualTo(3);
        assertThat(planItemInstances.stream().filter(p -> PlanItemDefinitionType.USER_EVENT_LISTENER.equals(p.getPlanItemDefinitionType()))
                .filter(p -> PlanItemInstanceState.AVAILABLE.equals(p.getState())).count()).isEqualTo(4);

        List<MilestoneInstance> milestoneInstances = cmmnRuntimeService.createMilestoneInstanceQuery().list();
        assertThat(milestoneInstances).isEmpty();

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

        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(3);

        //There are two named milestones
        MilestoneInstance abcMilestone = cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceName("abcMilestone").singleResult();
        assertThat(abcMilestone).isNotNull();
        assertThat(abcMilestone.getName()).isEqualTo("abcMilestone");

        MilestoneInstance xyzMilestone = cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceName("xyzMilestone").singleResult();
        assertThat(xyzMilestone).isNotNull();
        assertThat(xyzMilestone.getName()).isEqualTo("xyzMilestone");

        MilestoneInstance one = cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceName("1").singleResult();
        assertThat(one).isNotNull();
        assertThat(one.getName()).isEqualTo("1");

        //One Milestone has no name
        List<MilestoneInstance> list = cmmnRuntimeService.createMilestoneInstanceQuery().orderByMilestoneName().asc().list();
        assertThat(list)
                .extracting(MilestoneInstance::getName)
                .containsExactly("1", "abcMilestone", "xyzMilestone");

        //Query timestamps
        MilestoneInstance milestone1 = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeFirstTrigger)
                .milestoneInstanceReachedBefore(afterFirstTrigger)
                .singleResult();
        assertThat(milestone1).isNotNull();
        assertThat(milestone1.getElementId()).isEqualTo("milestonePlanItem1");
        MilestoneInstance milestone2 = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeSecondTrigger)
                .milestoneInstanceReachedBefore(afterSecondTrigger)
                .singleResult();
        assertThat(milestone2).isNotNull();
        assertThat(milestone2.getElementId()).isEqualTo("milestonePlanItem2");
        MilestoneInstance milestone3 = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceReachedAfter(beforeThirdTrigger)
                .milestoneInstanceReachedBefore(afterThirdTrigger)
                .singleResult();
        assertThat(milestone3).isNotNull();
        assertThat(milestone3.getElementId()).isEqualTo("milestonePlanItem3");

        list = cmmnRuntimeService.createMilestoneInstanceQuery().orderByTimeStamp().desc().list();
        assertThat(list)
                .extracting(MilestoneInstance::getElementId)
                .containsExactly("milestonePlanItem3", "milestonePlanItem2", "milestonePlanItem1");

        //Finish Case by triggering the last event
        event = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("event4").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(event.getId());
        assertCaseInstanceEnded(caseInstance);
    }
}
