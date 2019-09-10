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
package org.flowable.engine.test.bpmn.cache;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class CacheTaskTest extends PluggableFlowableTestCase {
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheTask.bpmn20.xml")
    public void testProcessInstanceAndExecutionIdInCache() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");

        assertNotNull(ServiceCacheTask.processInstanceId);
        assertEquals(processInstance.getId(), ServiceCacheTask.processInstanceId);
        assertNotNull(ServiceCacheTask.executionId);
        assertNotNull(ServiceCacheTask.historicProcessInstanceId);
        assertEquals(processInstance.getId(), ServiceCacheTask.historicProcessInstanceId);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheUserTask.bpmn20.xml")
    public void testTaskInCache() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        assertNotNull(CacheTaskListener.taskId);
        assertEquals(task.getId(), CacheTaskListener.taskId);
        assertNotNull(CacheTaskListener.historicTaskId);
        assertEquals(task.getId(), CacheTaskListener.historicTaskId);
    }
    
}
