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

package org.activiti.examples.bpmn.subprocess;

import java.util.List;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 */
public class SubProcessTest extends PluggableFlowableTestCase {

    public void testSimpleSubProcess() {

        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/examples/bpmn/subprocess/SubProcessTest.fixSystemFailureProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy();

        // After staring the process, both tasks in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fixSystemFailure");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .orderByTaskName()
                .asc()
                .list();

        // Tasks are ordered by name (see query)
        assertEquals(2, tasks.size());
        org.flowable.task.api.Task investigateHardwareTask = tasks.get(0);
        org.flowable.task.api.Task investigateSoftwareTask = tasks.get(1);
        assertEquals("Investigate hardware", investigateHardwareTask.getName());
        assertEquals("Investigate software", investigateSoftwareTask.getName());

        // Completing both the tasks finishes the subprocess and enables the task after the subprocess
        taskService.complete(investigateHardwareTask.getId());
        taskService.complete(investigateSoftwareTask.getId());

        org.flowable.task.api.Task writeReportTask = taskService
                .createTaskQuery()
                .processInstanceId(pi.getId())
                .singleResult();
        assertEquals("Write report", writeReportTask.getName());

        // Clean up
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

}
