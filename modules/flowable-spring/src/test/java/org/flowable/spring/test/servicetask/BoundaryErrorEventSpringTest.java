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

package org.flowable.spring.test.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Angel Lopez Cima
 * @author Falko Menge
 * @see <a href="https://activiti.atlassian.net/browse/ACT-1166">https://activiti.atlassian.net/browse/ACT-1166</a>
 */
@ContextConfiguration("classpath:org/flowable/spring/test/servicetask/serviceraskSpringTestCatchError-context.xml")
@DirtiesContext
public class BoundaryErrorEventSpringTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testCatchErrorThrownByJavaDelegateOnServiceTask() {
        String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByExpressionDelegateOnServiceTask").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    private void assertThatErrorHasBeenCaught(String procId) {
        // The service task will throw an error event,
        // which is caught on the service task boundary
        assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isEqualTo(1);
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }
}
