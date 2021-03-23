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

import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CacheTaskTest extends PluggableFlowableTestCase {
    
    @AfterEach
    void tearDown() {
        ServiceCacheTask.reset();
        TestCacheTaskListener.reset();
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheTask.bpmn20.xml")
    public void testProcessInstanceAndExecutionIdInCache() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");

        assertThat(ServiceCacheTask.processInstanceId).isEqualTo(processInstance.getId());
        assertThat(ServiceCacheTask.executionId).isNotNull();
        assertThat(ServiceCacheTask.historicProcessInstanceId).isEqualTo(processInstance.getId());
        assertThat(ServiceCacheTask.historicProcessInstanceDefinitionKey).isEqualTo("startToEnd");
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheUserTask.bpmn20.xml")
    public void testHistoricProcessInstanceDefinitionInformationWhenInCache() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");

        managementService.executeCommand(commandContext -> {
            String processInstanceId = processInstance.getId();

            // Make sure that it is loaded in the cache
            HistoricProcessInstance queriedHistoricProcess = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext)
                    .findById(processInstanceId);
            assertThat(queriedHistoricProcess.getProcessVariables()).isEmpty();

            queriedHistoricProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertThat(queriedHistoricProcess.getProcessDefinitionKey()).isEqualTo("oneTask");

            return null;
        });
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/cache/cacheUserTask.bpmn20.xml")
    public void testTaskInCache() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTask");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(TestCacheTaskListener.TASK_ID).isEqualTo(task.getId());
        assertThat(TestCacheTaskListener.HISTORIC_TASK_ID).isEqualTo(task.getId());
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

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/cache/cacheUserTaskAfterWaitState.bpmn20.xml")
    public void testTaskQueryWithIncludeVariablesAfterWaitState() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTask")
                .variable("var1", "Hello")
                .variable("var2", "World")
                .variable("var3", 123)
                .start();

        assertThat(TestCacheTaskListener.TASK_PROCESS_VARIABLES).isNull();
        assertThat(TestCacheTaskListener.HISTORIC_TASK_PROCESS_VARIABLES).isNull();

        assertThat(TestCacheTaskListener.TASK_LOCAL_VARIABLES).isNull();
        assertThat(TestCacheTaskListener.HISTORIC_TASK_LOCAL_VARIABLES).isNull();

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        Map.Entry[] entries = {
                entry("var1", "Hello"),
                entry("var2", "World"),
                entry("var3", 123),
                entry("varFromTheListener", "valueFromTheListener")
        };

        assertThat(TestCacheTaskListener.TASK_PROCESS_VARIABLES).containsOnly(entries);
        assertThat(TestCacheTaskListener.HISTORIC_TASK_PROCESS_VARIABLES).containsOnly(entries);

        assertThat(TestCacheTaskListener.TASK_LOCAL_VARIABLES).containsOnly(entry("localVar", "localValue"));
        assertThat(TestCacheTaskListener.HISTORIC_TASK_LOCAL_VARIABLES).containsOnly(entry("localVar", "localValue"));

        managementService.executeCommand(commandContext -> {
            // Make sure that it is loaded in the cache
            String taskId = TestCacheTaskListener.TASK_ID;
            Task queriedTask = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);
            assertThat(queriedTask.getProcessVariables()).isEmpty();
            assertThat(queriedTask.getTaskLocalVariables()).isEmpty();

            queriedTask = taskService.createTaskQuery().taskId(taskId).includeTaskLocalVariables().singleResult();

            assertThat(queriedTask.getProcessVariables()).isEmpty();
            assertThat(queriedTask.getTaskLocalVariables()).containsOnly(entry("localVar", "localValue"));

            queriedTask = taskService.createTaskQuery().taskId(taskId).includeProcessVariables().singleResult();

            assertThat(queriedTask.getProcessVariables()).containsOnly(entries);
            assertThat(queriedTask.getTaskLocalVariables()).containsOnly(entry("localVar", "localValue"));

            // Make sure that it is loaded in the cache
            HistoricTaskInstance queriedHistoricTask = processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().getHistoricTask(taskId);
            assertThat(queriedHistoricTask.getProcessVariables()).isEmpty();
            assertThat(queriedHistoricTask.getTaskLocalVariables()).isEmpty();

            queriedHistoricTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId).includeTaskLocalVariables().singleResult();

            assertThat(queriedHistoricTask.getProcessVariables()).isEmpty();
            assertThat(queriedHistoricTask.getTaskLocalVariables()).containsOnly(entry("localVar", "localValue"));

            queriedHistoricTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId).includeProcessVariables().singleResult();

            assertThat(queriedHistoricTask.getProcessVariables()).containsOnly(entries);
            assertThat(queriedHistoricTask.getTaskLocalVariables()).containsOnly(entry("localVar", "localValue"));

            return null;
        });

        managementService.executeCommand(commandContext -> {
            // Make sure that it is loaded in the cache
            String processInstanceId = processInstance.getId();
            ProcessInstance queriedProcessInstance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstanceId);
            assertThat(queriedProcessInstance.getProcessVariables()).isEmpty();

            queriedProcessInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();

            assertThat(queriedProcessInstance.getProcessVariables()).containsOnly(entries);

            // Make sure that it is loaded in the cache
            HistoricProcessInstance queriedHistoricProcess = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext)
                    .findById(processInstanceId);
            assertThat(queriedHistoricProcess.getProcessVariables()).isEmpty();

            queriedHistoricProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables()
                    .singleResult();
            assertThat(queriedHistoricProcess.getProcessVariables()).containsOnly(entries);

            return null;
        });

    }

}
