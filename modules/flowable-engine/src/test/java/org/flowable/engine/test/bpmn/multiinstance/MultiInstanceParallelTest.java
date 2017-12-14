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

package org.flowable.engine.test.bpmn.multiinstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;

public class MultiInstanceParallelTest extends PluggableFlowableTestCase {

    @Deployment
    public void testParallelUserTasks() {
        Map<String, Object> vars = new HashMap<>();
        List<String> assigneeList = Arrays.asList("kermit", "gonzo", "fozzie");
        vars.put("assigneeList", assigneeList);

        String procId = runtimeService.startProcessInstanceByKey("newmultiinstancetest", vars).getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        assertEquals("fozzie's Task", tasks.get(0).getName());
        assertEquals("gonzo's Task", tasks.get(1).getName());
        assertEquals("kermit's Task", tasks.get(2).getName());

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertProcessEnded(procId);
    }

    @Deployment
    public void testScriptVarParallelUserTasks() {
        String procId = runtimeService.startProcessInstanceByKey("newscriptvarmultiinstancetest").getId();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertEquals(3, tasks.size());
        assertEquals("fozzie's Task", tasks.get(0).getName());
        assertEquals("gonzo's Task", tasks.get(1).getName());
        assertEquals("kermit's Task", tasks.get(2).getName());

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        taskService.complete(tasks.get(2).getId());
        assertProcessEnded(procId);
    }
}
