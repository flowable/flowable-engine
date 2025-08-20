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
package ${package};

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.FlowableTest;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

@FlowableTest
class MyUnitTest {

    protected RuntimeService runtimeService;
    protected TaskService taskService;

    @BeforeEach
    void setUp(ProcessEngine processEngine) {
        runtimeService = processEngine.getRuntimeService();
    }

    @Test
    @Deployment(resources = {"${packageInPathFormat}/my-process.bpmn20.xml"})
    void test() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("my-process");
        assertThat(processInstance).isNotNull();

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Flowable is awesome!");
    }

}
