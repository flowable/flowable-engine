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

package org.flowable.spring.test.taskListener;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Yvo Swillens
 */
@ContextConfiguration("classpath:org/flowable/spring/test/taskListener/TaskListenerDelegateExpressionTest-context.xml")
public class TransactionDependentTaskListenerSpringTest extends SpringFlowableTestCase {

    @Autowired
    MyTransactionDependentTaskListener listener;

    @Test
    @Deployment
    public void testCustomPropertiesMapDelegateExpression() {
        runtimeService.startProcessInstanceByKey("transactionDependentTaskListenerProcess");

        // Completing first task will trigger the first closed listener (expression custom properties resolver)
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        assertThat(listener.getCurrentTasks().get(0).getTaskId()).isEqualTo("task3");
        assertThat(listener.getCurrentTasks().get(0).getCustomPropertiesMap())
                .containsEntry("customProp1", "task3");

        // Completing second task will trigger the second closed listener (delegate expression custom properties resolver)
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        assertThat(listener.getCurrentTasks().get(1).getTaskId()).isEqualTo("task4");
        assertThat(listener.getCurrentTasks().get(1).getCustomPropertiesMap())
                .containsEntry("customProp1", "task4");
    }

}
