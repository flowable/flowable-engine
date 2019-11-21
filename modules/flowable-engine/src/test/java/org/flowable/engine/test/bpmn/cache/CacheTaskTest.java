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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
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

        assertNotNull(TestCacheTaskListener.TASK_ID);
        assertEquals(task.getId(), TestCacheTaskListener.TASK_ID);
        assertNotNull(TestCacheTaskListener.HISTORIC_TASK_ID);
        assertEquals(task.getId(), TestCacheTaskListener.HISTORIC_TASK_ID);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheUserTask.bpmn20.xml")
    public void testProcessInstanceQueryWithIncludeVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTask")
            .variable("myVar1", "Hello")
            .variable("myVar2", "World")
            .variable("myVar3", 123)
            .start();

        Map.Entry[] entries = {
            entry("myVar1", "Hello"),
            entry("myVar2", "World"),
            entry("myVar3", 123),
            entry("varFromTheListener", "valueFromTheListener")
        };
        assertThat(processInstance.getProcessVariables()).containsOnly(entries);
        assertThat(TestCacheTaskListener.PROCESS_VARIABLES).containsOnly(entries);
        assertThat(TestCacheTaskListener.HISTORIC_PROCESS_VARIABLES).containsOnly(entries);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheUserTask.bpmn20.xml")
    public void testTaskQueryWithIncludeVariables() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTask")
            .variable("myVar1", "Hello")
            .variable("myVar2", "World")
            .variable("myVar3", 123)
            .start();

        Map.Entry[] entries = {
            entry("myVar1", "Hello"),
            entry("myVar2", "World"),
            entry("myVar3", 123),
            entry("varFromTheListener", "valueFromTheListener")
        };

        assertThat(TestCacheTaskListener.TASK_PROCESS_VARIABLES).containsOnly(entries);
        assertThat(TestCacheTaskListener.HISTORIC_TASK_PROCESS_VARIABLES).containsOnly(entries);

        assertThat(TestCacheTaskListener.TASK_LOCAL_VARIABLES).containsOnly(entry("localVar", "localValue"));
        assertThat(TestCacheTaskListener.HISTORIC_TASK_LOCAL_VARIABLES).containsOnly(entry("localVar", "localValue"));

    }

}
