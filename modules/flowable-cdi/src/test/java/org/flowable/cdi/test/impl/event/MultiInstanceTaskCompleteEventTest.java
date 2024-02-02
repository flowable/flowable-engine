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
package org.flowable.cdi.test.impl.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Test;

public class MultiInstanceTaskCompleteEventTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/MultiInstanceTaskCompleteEventTest.process1.bpmn20.xml.bpmn" })
    public void testReceiveAll() {

        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertThat(listenerBean.getCreateTask1()).isZero();
        assertThat(listenerBean.getAssignTask1()).isZero();
        assertThat(listenerBean.getCompleteTask1()).isZero();

        // start the process
        runtimeService.startProcessInstanceByKey("process1");

        Task task = taskService.createTaskQuery().singleResult();

        taskService.claim(task.getId(), "auser");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // assert
        assertThat(listenerBean.getCreateTask1()).isEqualTo(2);
        assertThat(listenerBean.getAssignTask1()).isEqualTo(1);
        assertThat(listenerBean.getCompleteTask1()).isEqualTo(2);

    }
}
