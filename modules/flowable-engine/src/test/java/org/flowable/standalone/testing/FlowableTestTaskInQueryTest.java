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

package org.flowable.standalone.testing;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import java.util.List;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;




/**
 * @author 盘古BPM
 * @author 分享牛
 */
public class FlowableTestTaskInQueryTest extends FlowableTestCase {

    @Deployment
    public void testSimpleProcess() {
        runtimeService.startProcessInstanceByKey("simpleProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("My Task");
        List<Task> list = taskService.createTaskQuery()
                .taskIdIn(Collections.singleton(task.getId())).list();
        assertThat(list.size()).isEqualTo(1);
        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        List<HistoricTaskInstance> historicTaskInstanceList = historicDataService.createHistoricTaskInstanceQuery()
                .taskIdIn(Collections.singleton(task.getId())).list();
        assertThat(historicTaskInstanceList.size()).isEqualTo(1);


    }
}
