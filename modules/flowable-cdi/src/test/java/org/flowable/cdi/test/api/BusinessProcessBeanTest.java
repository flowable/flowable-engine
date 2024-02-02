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
package org.flowable.cdi.test.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.BusinessProcess;
import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Daniel Meyer
 */
public class BusinessProcessBeanTest extends CdiFlowableTestCase {

    /* General test asserting that the business process bean is functional */
    @Test
    @Deployment
    public void test() throws Exception {

        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        // start the process
        businessProcess.startProcessByKey("businessProcessBeanTest").getId();

        // ensure that the process is started:
        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNotNull();

        // ensure that there is a single task waiting
        Task task = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        String value = "value";
        businessProcess.setVariable("key", value);
        assertThat((String) businessProcess.getVariable("key")).isEqualTo(value);

        // complete the task
        assertThat(businessProcess.startTask(task.getId()).getId()).isEqualTo(task.getId());
        businessProcess.completeTask();

        // assert the task is completed
        assertThat(processEngine.getTaskService().createTaskQuery().singleResult()).isNull();

        // assert that the process is ended:
        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNull();

    }

    @Test
    @Deployment
    public void testProcessWithoutWaitState() {
        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        // start the process
        businessProcess.startProcessByKey("businessProcessBeanTest").getId();

        // assert that the process is ended:
        assertThat(processEngine.getRuntimeService().createProcessInstanceQuery().singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
    public void testResolveProcessInstanceBean() {
        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        assertThat(getBeanInstance(ProcessInstance.class)).isNull();
        assertThat(getBeanInstance("processInstanceId")).isNull();
        assertThat(getBeanInstance(Execution.class)).isNull();
        assertThat(getBeanInstance("executionId")).isNull();

        String pid = businessProcess.startProcessByKey("businessProcessBeanTest").getId();

        // assert that now we can resolve the ProcessInstance-bean
        assertThat(getBeanInstance(ProcessInstance.class).getId()).isEqualTo(pid);
        assertThat(getBeanInstance("processInstanceId")).isEqualTo(pid);
        assertThat(getBeanInstance(Execution.class).getId()).isEqualTo(pid);
        assertThat(getBeanInstance("executionId")).isEqualTo(pid);

        taskService.complete(taskService.createTaskQuery().singleResult().getId());
    }

    @Test
    @Deployment(resources = "org/flowable/cdi/test/api/BusinessProcessBeanTest.test.bpmn20.xml")
    public void testResolveTaskBean() {
        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        assertThat(getBeanInstance(Task.class)).isNull();
        assertThat(getBeanInstance("taskId")).isNull();

        businessProcess.startProcessByKey("businessProcessBeanTest");
        String taskId = taskService.createTaskQuery().singleResult().getId();

        businessProcess.startTask(taskId);

        // assert that now we can resolve the Task-bean
        assertThat(getBeanInstance(Task.class).getId()).isEqualTo(taskId);
        assertThat(getBeanInstance("taskId")).isEqualTo(taskId);

        taskService.complete(taskService.createTaskQuery().singleResult().getId());
    }
}
