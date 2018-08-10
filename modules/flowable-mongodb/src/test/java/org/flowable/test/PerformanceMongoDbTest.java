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

import java.util.Collections;
import java.util.List;

import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class PerformanceMongoDbTest extends AbstractMongoDbTest {
    
    @Test
    public void testPerformance() {
        repositoryService.createDeployment().addClasspathResource("performanceProcess.bpmn20.xml").deploy();
        
        for (int i = 0; i < 10; i++) {
            runtimeService.startProcessInstanceByKey("gateway", Collections.singletonMap("gotoFirstTask", true));
        }
        
        for (int i = 0; i < 10; i++) {
            runtimeService.startProcessInstanceByKey("gateway", Collections.singletonMap("gotoFirstTask", false));
        }
        
        assertEquals(20, runtimeService.createProcessInstanceQuery().count());
        
        for (int i = 0; i < 10; i++) {
            List<Task> tasks = taskService.createTaskQuery().taskAssignee("johndoe").listPage(0, 20);
            assertEquals("my task", tasks.get(0).getName());
            assertEquals(10, taskService.createTaskQuery().taskAssignee("johndoe").count());
        }
        
        List<Task> tasks = taskService.createTaskQuery().taskAssignee("johndoe").list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
        
        assertEquals(20, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, taskService.createTaskQuery().taskAssignee("johndoe").count());
        assertEquals(10, taskService.createTaskQuery().taskAssignee("janedoe").count());
        
        tasks = taskService.createTaskQuery().taskUnassigned().list();
        for (Task task : tasks) {
            taskService.claim(task.getId(), "janedoe");
        }
        
        assertEquals(20, taskService.createTaskQuery().taskAssignee("janedoe").count());
        tasks = taskService.createTaskQuery().taskAssignee("janedoe").list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
        
        assertEquals(10, runtimeService.createProcessInstanceQuery().count());
        assertEquals(10, taskService.createTaskQuery().taskAssignee("johndoe").count());
        tasks = taskService.createTaskQuery().taskAssignee("johndoe").list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
        
        assertEquals(0, taskService.createTaskQuery().count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }
    
}
