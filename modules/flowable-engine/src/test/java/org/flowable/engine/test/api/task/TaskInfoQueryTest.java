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
package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskInfoQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class TaskInfoQueryTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() throws Exception {
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.deleteTask(task.getId(), true);
        }
    }

    @Test
    public void testTaskInfoQuery() {
        Date now = processEngineConfiguration.getClock().getCurrentTime();

        // 4 tasks with a due date
        createTask("task0", new Date(now.getTime() + (4L * 7L * 24L * 60L * 60L * 1000L))); // 4 weeks in future
        createTask("task1", new Date(now.getTime() + (2 * 24L * 60L * 60L * 1000L))); // 2 days in future
        createTask("task2", new Date(now.getTime() + (7L * 24L * 60L * 60L * 1000L))); // 1 week in future
        createTask("task3", new Date(now.getTime() + (24L * 60L * 60L * 1000L))); // 1 day in future

        // 2 tasks without a due date
        createTask("task4", null);
        createTask("task5", null);

        // Runtime
        TaskInfoQueryWrapper taskInfoQueryWrapper = new TaskInfoQueryWrapper(taskService.createTaskQuery());
        List<? extends TaskInfo> taskInfos = taskInfoQueryWrapper.getTaskInfoQuery().or().taskNameLike("%k1%")
                .taskDueAfter(new Date(now.getTime() + (3 * 24L * 60L * 60L * 1000L))).endOr().list();

        assertThat(taskInfos).hasSize(3);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            taskInfoQueryWrapper = new TaskInfoQueryWrapper(historyService.createHistoricTaskInstanceQuery());
            taskInfos = taskInfoQueryWrapper.getTaskInfoQuery().or().taskNameLike("%k1%").taskDueAfter(new Date(now.getTime() + (3 * 24L * 60L * 60L * 1000L)))
                    .endOr().list();

            assertThat(taskInfos).hasSize(3);
        }
    }

    private org.flowable.task.api.Task createTask(String name, Date dueDate) {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName(name);
        task.setDueDate(dueDate);
        taskService.saveTask(task);
        return task;
    }

}
