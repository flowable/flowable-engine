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
package org.activiti.engine.test.regression;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.drools.core.util.StringUtils;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;

public class SignalV6andV5Test extends PluggableFlowableTestCase {

    public void testBoundarySignalFromV6ToV5() throws Exception {

        // Deploy processes
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalBoundaryCatch.bpmn")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();
        
        String deploymentId2 = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalThrow.bpmn")
                .deploy()
                .getId();

        runtimeService.startProcessInstanceByKey("signalBoundaryCatch");
        runtimeService.startProcessInstanceByKey("signalThrow");
        
        ProcessDefinition v5Definition = repositoryService.createProcessDefinitionQuery().processDefinitionEngineVersion("v5").singleResult();
        assertNotNull(v5Definition);
        assertEquals("signalBoundaryCatch", v5Definition.getKey());
        
        ProcessDefinition v6Definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("signalThrow").singleResult();
        assertNotNull(v6Definition);
        assertTrue(StringUtils.isEmpty(v6Definition.getEngineVersion()));
        
        org.flowable.task.api.Task signalTask = taskService.createTaskQuery().processDefinitionKey("signalBoundaryCatch").singleResult();
        assertNotNull(signalTask);
        assertEquals("task", signalTask.getTaskDefinitionKey());
        
        org.flowable.task.api.Task beforeTask = taskService.createTaskQuery().processDefinitionKey("signalThrow").singleResult();
        taskService.complete(beforeTask.getId());
        
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processDefinitionKey("signalBoundaryCatch").singleResult();
        assertNotNull(afterTask);
        assertEquals("afterTask", afterTask.getTaskDefinitionKey());
        
        // Clean
        repositoryService.deleteDeployment(deploymentId, true);
        repositoryService.deleteDeployment(deploymentId2, true);
    }
    
    public void testIntermediateSignalFromV6ToV5() throws Exception {

        // Deploy processes
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalIntermediateCatch.bpmn")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();
        
        String deploymentId2 = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalThrow.bpmn")
                .deploy()
                .getId();

        runtimeService.startProcessInstanceByKey("signalIntermediateCatch");
        runtimeService.startProcessInstanceByKey("signalThrow");
        
        ProcessDefinition v5Definition = repositoryService.createProcessDefinitionQuery().processDefinitionEngineVersion("v5").singleResult();
        assertNotNull(v5Definition);
        assertEquals("signalIntermediateCatch", v5Definition.getKey());
        
        ProcessDefinition v6Definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("signalThrow").singleResult();
        assertNotNull(v6Definition);
        assertTrue(StringUtils.isEmpty(v6Definition.getEngineVersion()));
        
        org.flowable.task.api.Task signalTask = taskService.createTaskQuery().processDefinitionKey("signalIntermediateCatch").singleResult();
        assertNotNull(signalTask);
        assertEquals("task", signalTask.getTaskDefinitionKey());
        taskService.complete(signalTask.getId());
        
        org.flowable.task.api.Task beforeTask = taskService.createTaskQuery().processDefinitionKey("signalThrow").singleResult();
        taskService.complete(beforeTask.getId());
        
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processDefinitionKey("signalIntermediateCatch").singleResult();
        assertNotNull(afterTask);
        assertEquals("afterTask", afterTask.getTaskDefinitionKey());
        
        // Clean
        repositoryService.deleteDeployment(deploymentId, true);
        repositoryService.deleteDeployment(deploymentId2, true);
    }
    
    public void testBoundarySignalFromV5ToV6() throws Exception {

        // Deploy processes
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalBoundaryCatch.bpmn")
                .deploy()
                .getId();
        
        String deploymentId2 = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalThrow.bpmn")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();

        runtimeService.startProcessInstanceByKey("signalBoundaryCatch");
        runtimeService.startProcessInstanceByKey("signalThrow");
        
        ProcessDefinition v5Definition = repositoryService.createProcessDefinitionQuery().processDefinitionEngineVersion("v5").singleResult();
        assertNotNull(v5Definition);
        assertEquals("signalThrow", v5Definition.getKey());
        
        ProcessDefinition v6Definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("signalBoundaryCatch").singleResult();
        assertNotNull(v6Definition);
        assertTrue(StringUtils.isEmpty(v6Definition.getEngineVersion()));
        
        org.flowable.task.api.Task signalTask = taskService.createTaskQuery().processDefinitionKey("signalBoundaryCatch").singleResult();
        assertNotNull(signalTask);
        assertEquals("task", signalTask.getTaskDefinitionKey());
        
        org.flowable.task.api.Task beforeTask = taskService.createTaskQuery().processDefinitionKey("signalThrow").singleResult();
        taskService.complete(beforeTask.getId());
        
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processDefinitionKey("signalBoundaryCatch").singleResult();
        assertNotNull(afterTask);
        assertEquals("afterTask", afterTask.getTaskDefinitionKey());
        
        // Clean
        repositoryService.deleteDeployment(deploymentId, true);
        repositoryService.deleteDeployment(deploymentId2, true);
    }
    
    public void testIntermediateSignalFromV5ToV6() throws Exception {

        // Deploy processes
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalIntermediateCatch.bpmn")
                .deploy()
                .getId();
        
        String deploymentId2 = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/regression/signalThrow.bpmn")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();

        runtimeService.startProcessInstanceByKey("signalIntermediateCatch");
        runtimeService.startProcessInstanceByKey("signalThrow");
        
        ProcessDefinition v5Definition = repositoryService.createProcessDefinitionQuery().processDefinitionEngineVersion("v5").singleResult();
        assertNotNull(v5Definition);
        assertEquals("signalThrow", v5Definition.getKey());
        
        ProcessDefinition v6Definition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("signalIntermediateCatch").singleResult();
        assertNotNull(v6Definition);
        assertTrue(StringUtils.isEmpty(v6Definition.getEngineVersion()));
        
        org.flowable.task.api.Task signalTask = taskService.createTaskQuery().processDefinitionKey("signalIntermediateCatch").singleResult();
        assertNotNull(signalTask);
        assertEquals("task", signalTask.getTaskDefinitionKey());
        taskService.complete(signalTask.getId());
        
        org.flowable.task.api.Task beforeTask = taskService.createTaskQuery().processDefinitionKey("signalThrow").singleResult();
        taskService.complete(beforeTask.getId());
        
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processDefinitionKey("signalIntermediateCatch").singleResult();
        assertNotNull(afterTask);
        assertEquals("afterTask", afterTask.getTaskDefinitionKey());
        
        // Clean
        repositoryService.deleteDeployment(deploymentId, true);
        repositoryService.deleteDeployment(deploymentId2, true);
    }

}
