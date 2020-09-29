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

package org.flowable.engine.test.bpmn.event.message;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Tijs Rademakers
 */
public class MessageNonInterruptingBoundaryEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSingleNonInterruptingBoundaryMessageEvent() {
        runtimeService.startProcessInstanceByKey("process");

        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(3);

        org.flowable.task.api.Task userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        assertThat(userTask).isNotNull();

        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(execution).isNotNull();

        // 1. case: message received before completing the task

        runtimeService.messageEventReceived("messageName", execution.getId());
        // event subscription not removed
        execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(execution).isNotNull();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
        assertThat(userTask).isNotNull();
        assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
        taskService.complete(userTask.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        // send a message a second time
        runtimeService.messageEventReceived("messageName", execution.getId());
        // event subscription not removed
        execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(execution).isNotNull();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessage").singleResult();
        assertThat(userTask).isNotNull();
        assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterMessage");
        taskService.complete(userTask.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        // now complete the user task with the message boundary event
        userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        assertThat(userTask).isNotNull();

        taskService.complete(userTask.getId());

        // event subscription removed
        execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(execution).isNull();

        userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
        assertThat(userTask).isNotNull();

        taskService.complete(userTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        // 2nd. case: complete the user task cancels the message subscription

        runtimeService.startProcessInstanceByKey("process");

        userTask = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        assertThat(userTask).isNotNull();
        taskService.complete(userTask.getId());

        execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("messageName").singleResult();
        assertThat(execution).isNull();

        userTask = taskService.createTaskQuery().taskDefinitionKey("taskAfterTask").singleResult();
        assertThat(userTask).isNotNull();
        assertThat(userTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
        taskService.complete(userTask.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

}
