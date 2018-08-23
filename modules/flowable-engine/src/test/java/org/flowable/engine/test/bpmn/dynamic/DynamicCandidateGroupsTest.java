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
package org.flowable.engine.test.bpmn.dynamic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;

import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Pardo David on 7/12/2016.
 */
public class DynamicCandidateGroupsTest extends PluggableFlowableTestCase implements DynamicBpmnConstants {

    private static final String TASK_ONE_SID = "sid-B94D5D22-E93E-4401-ADC5-C5C073E1EEB4";

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testIsShouldBePossibleToChangeCandidateGroups() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        ArrayList<String> candidateGroups = new ArrayList<>(2);
        candidateGroups.add("HR");
        candidateGroups.add("SALES");

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroups(TASK_ONE_SID, candidateGroups);
        dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(), processInfo);

        runtimeService.startProcessInstanceByKey("dynamicServiceTest");

        long hrTaskCount = taskService.createTaskQuery().taskCandidateGroup("HR").count();
        long salesTaskCount = taskService.createTaskQuery().taskCandidateGroup("SALES").count();

        assertThat(hrTaskCount, is(1L));
        assertThat(salesTaskCount, is(1L));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testIsShouldBePossibleToResetChangeCandidateGroups() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        ArrayList<String> candidateGroups = new ArrayList<>(2);
        candidateGroups.add("HR");
        candidateGroups.add("SALES");

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroups(TASK_ONE_SID, candidateGroups);
        dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(), processInfo);

        // reset
        dynamicBpmnService.resetProperty(TASK_ONE_SID, USER_TASK_CANDIDATE_GROUPS, processInfo);
        dynamicBpmnService.saveProcessDefinitionInfo(instance.getProcessDefinitionId(), processInfo);

        runtimeService.startProcessInstanceByKey("dynamicServiceTest");

        long hrTaskCount = taskService.createTaskQuery().taskCandidateGroup("HR").count();
        long salesTaskCount = taskService.createTaskQuery().taskCandidateGroup("SALES").count();

        assertThat(hrTaskCount, is(0L));
        assertThat(salesTaskCount, is(0L));
    }
}
