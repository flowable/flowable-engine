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

package org.flowable.engine.test.api.history;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;





/**
 * @author pangubpm
 */
public class HistoryServiceTaskIdInTest extends PluggableFlowableTestCase {


    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"})
    public void testTaskServiceTaskIdInQuery() {
        String processDefinitionKey = "oneTaskProcess";
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        String id = task.getId();
        List<Task> list = taskService.createTaskQuery().taskIdIn(Collections.singleton(id)).list();
        assertThat(list).hasSize(1);

    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"})
    public void testHistoryServiceTaskIdInQuery() {
        String processDefinitionKey = "oneTaskProcess";
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
        String id = task.getId();
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .taskIdIn(Collections.singleton(id)).list();
        assertThat(list).hasSize(1);

    }

}
