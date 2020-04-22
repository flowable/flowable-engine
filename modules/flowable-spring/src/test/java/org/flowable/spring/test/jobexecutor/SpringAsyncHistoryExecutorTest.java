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
package org.flowable.spring.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.test.CleanTest;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@CleanTest
// We need to use per class as the test uses auto deployments. If they are deleted then the other tests will fail
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration("classpath:org/flowable/spring/test/components/SpringAsyncHistoryJobExecutorTest-context.xml")
public class SpringAsyncHistoryExecutorTest extends SpringFlowableTestCase {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Test
    public void testHistoryDataGenerated() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        while (!tasks.isEmpty()) {
            taskService.complete(tasks.get(0).getId());
            tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        }

        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 20000L, 200L, false);

        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstance.getId()).orderByTaskName().asc().list();

        assertThat(historicTasks)
                .extracting(HistoricTaskInstance::getName)
                .containsExactly("Task a", "Task b1", "Task b2", "Task c");
    }

}
