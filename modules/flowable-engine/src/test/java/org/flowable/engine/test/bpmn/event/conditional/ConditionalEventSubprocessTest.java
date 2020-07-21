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

package org.flowable.engine.test.bpmn.event.conditional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class ConditionalEventSubprocessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(8);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test2"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(8);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        
        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // we still have 7 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now let's complete the task in the event subprocess
        Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testNonInterruptingNestedSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(6);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(9);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test2"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(9);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        
        // now let's first complete the task in the main flow:
        Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // we still have 8 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(8);

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testInterruptingNestedSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("myVar", "test"));
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now let's complete the task in the event subprocess
        Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testSimpleInterruptingEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleConditionalEventSubProcess");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task")
            );

        // Evaluate conditional events
        runtimeService.evaluateConditionalEvents(processInstance.getId(), Collections.singletonMap("testVar", true));

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "conditionalEventSubProcess"),
                tuple("startEvent", "eventSubProcessConditionalStart"),
                tuple("sequenceFlow", "eventSubProcessFlow1"),
                tuple("userTask", "eventSubProcessTask1")
            );

        // Complete the user task in the event sub process
        Task eventSubProcessTask = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask1").singleResult();
        assertThat(eventSubProcessTask).isNotNull();
        taskService.complete(eventSubProcessTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                    tuple("startEvent", "start"),
                    tuple("sequenceFlow", "flow1"),
                    tuple("userTask", "task"),
                    tuple("eventSubProcess", "conditionalEventSubProcess"),
                    tuple("startEvent", "eventSubProcessConditionalStart"),
                    tuple("sequenceFlow", "eventSubProcessFlow1"),
                    tuple("userTask", "eventSubProcessTask1"),
                    tuple("sequenceFlow", "eventSubProcessFlow2"),
                    tuple("endEvent", "eventSubProcessEnd")
                );
        }

        assertProcessEnded(processInstance.getId());
    }

}
