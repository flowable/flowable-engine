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
package org.flowable.engine.configurator.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class ProcessTest extends FlowableAppTestCase {
    
    @Test
    public void testCompleteTask() throws Exception {
        ProcessEngineConfiguration processEngineConfiguration = (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();
        
        AppDeployment deployment = appRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/configurator/test/oneTaskProcess.bpmn20.xml").deploy();
        
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);
            
            runtimeService.addUserIdentityLink(processInstance.getId(), "anotherUser", IdentityLinkType.STARTER);
            taskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
            
            assertEquals(2, runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()).size());
            assertEquals(1, taskService.getIdentityLinksForTask(task.getId()).size());
            
            taskService.complete(task.getId());
            
            try {
                assertEquals(0, runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()).size());
                fail("object not found expected");
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
            try {
                assertEquals(0, taskService.getIdentityLinksForTask(task.getId()).size());
                fail("object not found expected");
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
            assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            
            
        } finally {
            appRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    public void testCompleteTaskWithForm() throws Exception {
        ProcessEngineConfiguration processEngineConfiguration = (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();
        
        AppDeployment deployment = appRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/configurator/test/oneTaskWithFormProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/engine/configurator/test/simple.form").deploy();
        
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);
            
            runtimeService.addUserIdentityLink(processInstance.getId(), "anotherUser", IdentityLinkType.STARTER);
            taskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
            
            assertEquals(2, runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()).size());
            assertEquals(1, taskService.getIdentityLinksForTask(task.getId()).size());
            
            FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                            .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
            FormDefinition formDefinition = formEngineConfiguration.getFormRepositoryService().createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertNotNull(formDefinition);
            
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("input1", "test");
            taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), null, variables);
            
            try {
                assertEquals(0, runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()).size());
                fail("object not found expected");
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
            try {
                assertEquals(0, taskService.getIdentityLinksForTask(task.getId()).size());
                fail("object not found expected");
            } catch (FlowableObjectNotFoundException e) {
                // expected
            }
            
            assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            
            
        } finally {
            appRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testCompleteTaskWithAnotherForm() {
        ProcessEngineConfiguration processEngineConfiguration = (ProcessEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                        .get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        RuntimeService runtimeService = processEngineConfiguration.getRuntimeService();
        TaskService taskService = processEngineConfiguration.getTaskService();

        AppDeployment deployment = appRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/configurator/test/oneTaskWithFormProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/engine/configurator/test/another.form")
            .addClasspathResource("org/flowable/engine/configurator/test/simple.form").deploy();

        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertNotNull(task);

            runtimeService.addUserIdentityLink(processInstance.getId(), "anotherUser", IdentityLinkType.STARTER);
            taskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);

            assertEquals(2, runtimeService.getIdentityLinksForProcessInstance(processInstance.getId()).size());
            assertEquals(1, taskService.getIdentityLinksForTask(task.getId()).size());

            FormEngineConfiguration formEngineConfiguration = (FormEngineConfiguration) appEngineConfiguration.getEngineConfigurations()
                            .get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
            FormDefinition formDefinition = formEngineConfiguration.getFormRepositoryService().createFormDefinitionQuery().formDefinitionKey("anotherForm").singleResult();
            assertNotNull(formDefinition);

            Map<String, Object> variables = new HashMap<>();
            variables.put("anotherInput", "test");
            taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), null, variables);

            assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());


        } finally {
            appRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}
