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
package org.flowable.cmmn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CaseTaskTest extends AbstractProcessEngineIntegrationTest {
    
    @Test
    @CmmnDeployment
    public void testCaseTask() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcess.bpmn20.xml")
                .deploy();
        
        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask");
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngineTaskService.complete(processTasks.get(0).getId());
            
            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                            .processInstanceId(processInstance.getId())
                            .singleResult();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                            .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                            .singleResult();
            
            assertNotNull(caseInstance);
            
            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertEquals(1, caseTasks.size());
            
            cmmnTaskService.complete(caseTasks.get(0).getId());
            
            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNull(dbCaseInstance);
            
            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
            
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseTaskWithParameters() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/caseTaskProcessWithParameters.bpmn20.xml")
                .deploy();
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("testVar", "test");
            variables.put("testNumVar", 43);
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseTask", variables);
            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            processEngineTaskService.complete(processTasks.get(0).getId());
            
            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                            .processInstanceId(processInstance.getId())
                            .singleResult();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId(execution.getId())
                            .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                            .singleResult();
            
            assertNotNull(caseInstance);
            
            assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestVar"));
            assertEquals(43, cmmnRuntimeService.getVariable(caseInstance.getId(), "caseTestNumVar"));
            
            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertEquals(1, caseTasks.size());
            
            cmmnRuntimeService.setVariable(caseInstance.getId(), "caseResult", "someResult");
            
            cmmnTaskService.complete(caseTasks.get(0).getId());
            
            CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNull(dbCaseInstance);
            
            processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(1, processTasks.size());
            
            assertEquals("someResult", processEngineRuntimeService.getVariable(processInstance.getId(), "processResult"));
            
            processEngine.getTaskService().complete(processTasks.get(0).getId());
            assertEquals(0, processEngineRuntimeService.createProcessInstanceQuery().count());
            
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}
