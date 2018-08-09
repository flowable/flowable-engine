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
package org.flowable.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class SubProcessMongoDbTest extends AbstractMongoDbTest {
    
    @Test
    public void testNestedSubProcess() {
        repositoryService.createDeployment().addClasspathResource("nestedSubProcess.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSubprocesses");
        
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(5, executions.size());
        
        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        
        taskService.complete(tasks.get(0).getId());
        
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(5, executions.size());
        
        taskService.complete(tasks.get(1).getId());
        
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(0, executions.size());
        
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

}
