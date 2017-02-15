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
package org.flowable.compatibility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Test;

public class StartProcessInstanceTest extends AbstractFlowable6CompatibilityTest {

    @Test
    public void testStartProcessInstance() {

        // There should be one task active for the process, from the v5 test data generator
        assertEquals(1, taskService.createTaskQuery().processInstanceBusinessKey("activitiv5-one-task-process").count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals(1, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").count());

        // Starting a new process instance will start it in v5 mode
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(2, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals(2, taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").count());

        // For v5, there should be only one execution
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // Completing a task in v5 mode
        taskService.complete(taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list().get(0).getId());
        assertEquals(1, taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());

        // Deploying the process definition again. But not yet ready to migrate to v6...
        repositoryService.createDeployment()
                .addClasspathResource("oneTaskProcess.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy();

        assertEquals(2, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").count());
        for (ProcessDefinition processDefinition : repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list()) {
            assertEquals("v5", ((ProcessDefinitionEntity) processDefinition).getEngineVersion());
        }

        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(2, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
        assertEquals(2, taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").count());

        // The process definition has been migrated to v6. Deploying it as a 6 process definition
        repositoryService.createDeployment()
                .addClasspathResource("oneTaskProcess.bpmn20.xml")
                .deploy();
        assertEquals(3, repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").count());
        assertNull(((ProcessDefinitionEntity) repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").latestVersion().singleResult()).getEngineVersion());

        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertEquals(3, taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").count());

        // For v6, we expect 2 execution (vs 1 in v5)
        assertEquals(2, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyProcessInstanceExecutions().count());

    }

}
