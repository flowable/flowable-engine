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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class TimerMongoDbTest extends AbstractMongoDbTest {
    
    @Test
    public void testBoundaryTimerEventCompleteTask() {
        repositoryService.createDeployment().addClasspathResource("timerTask.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerTask");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("theUserTask", task.getTaskDefinitionKey());
        
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        
        taskService.complete(task.getId());
        assertEquals(0, managementService.createJobQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }
    
    @Test
    public void testBoundaryTimerEventExecuteTimer() {
        repositoryService.createDeployment().addClasspathResource("timerTask.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerTask");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("theUserTask", task.getTaskDefinitionKey());
        
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());
        
        assertEquals(0, managementService.createJobQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("afterTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        
        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }
    
}
