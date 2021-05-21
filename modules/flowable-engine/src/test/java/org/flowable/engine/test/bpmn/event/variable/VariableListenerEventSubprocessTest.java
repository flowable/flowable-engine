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

package org.flowable.engine.test.bpmn.event.variable;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class VariableListenerEventSubprocessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        runtimeService.setVariable(processInstance.getId(), "var1", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().count()).isZero();

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testInterruptingSubProcessDifferentVariableName() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        runtimeService.setVariable(processInstance.getId(), "var2", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);

        // now let's complete the task in the process
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testInterruptingSubProcessCreateChangeType() {
        // trigger variable listener right with the start process instance
        
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("var1", "test")
                .start();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        // trigger non interrupting event sub process
        runtimeService.setVariable(processInstance.getId(), "var1", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // trigger again non interrupting event sub process
        runtimeService.setVariable(processInstance.getId(), "var1", "test2");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        // variable is different from variable listener
        runtimeService.setVariable(processInstance.getId(), "var2", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);

        // now let's complete the tasks in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().iterator().next();
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());
        
        // complete root process task
        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testSubProcessAndEventSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("variableListenerProcess")
                .variable("var2", "initial")
                .start();
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(4);
        
        runtimeService.signalEventReceived("signalTest");
        
        assertThat(runtimeService.getVariable(processInstance.getId(), "var2")).isEqualTo("test");
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("formTask3");
        
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testInterruptingSubProcessMultipleVariableListeners() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        runtimeService.setVariable(processInstance.getId(), "var2", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().count()).isZero();

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessVar2Task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testNonInterruptingSubProcessMultipleVariableListeners() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        // trigger non interrupting event sub process
        runtimeService.setVariable(processInstance.getId(), "var1", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(6);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        // trigger again non interrupting event sub process
        runtimeService.setVariable(processInstance.getId(), "var2", "test2");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(8);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);
        
        // variable is different from variable listener
        runtimeService.setVariable(processInstance.getId(), "var3", "test");

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(8);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(2);

        // now let's complete the tasks in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessVar2Task").singleResult();
        taskService.complete(task.getId());
        
        // complete root process task
        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testInterruptingSubProcessByServiceTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().count()).isZero();

        // now let's complete the task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

}
