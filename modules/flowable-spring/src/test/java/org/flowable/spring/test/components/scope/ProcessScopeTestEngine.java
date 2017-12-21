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
package org.flowable.spring.test.components.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.util.StringUtils;

class ProcessScopeTestEngine {
    private int customerId = 43;

    private String keyForObjectType(Map<String, Object> runtimeVars, Class<?> clazz) {
        for (Map.Entry<String, Object> e : runtimeVars.entrySet()) {
            Object value = e.getValue();
            if (value.getClass().isAssignableFrom(clazz)) {
                return e.getKey();
            }
        }
        return null;
    }

    private StatefulObject run() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerId", customerId);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("component-waiter", vars);

        Map<String, Object> runtimeVars = runtimeService.getVariables(processInstance.getId());

        String statefulObjectVariableKey = keyForObjectType(runtimeVars, StatefulObject.class);

        assertTrue(!runtimeVars.isEmpty());
        assertTrue(StringUtils.hasText(statefulObjectVariableKey));

        StatefulObject scopedObject = (StatefulObject) runtimeService.getVariable(processInstance.getId(), statefulObjectVariableKey);
        assertNotNull(scopedObject);
        assertTrue(StringUtils.hasText(scopedObject.getName()));
        assertEquals(2, scopedObject.getVisitedCount());

        // the process has paused
        String procId = processInstance.getProcessInstanceId();

        List<Task> tasks = taskService.createTaskQuery().executionId(procId).list();
        assertEquals(1, tasks.size());

        Task t = tasks.iterator().next();
        this.taskService.claim(t.getId(), "me");
        this.taskService.complete(t.getId());

        scopedObject = (StatefulObject) runtimeService.getVariable(processInstance.getId(), statefulObjectVariableKey);
        assertEquals(3, scopedObject.getVisitedCount());

        assertEquals(customerId, scopedObject.getCustomerId());
        return scopedObject;
    }

    private ProcessEngine processEngine;
    private RuntimeService runtimeService;
    private TaskService taskService;

    public void testScopedProxyCreation() {

        StatefulObject one = run();
        StatefulObject two = run();
        assertNotSame(one.getName(), two.getName());
        assertEquals(one.getVisitedCount(), two.getVisitedCount());
    }

    public ProcessScopeTestEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        this.runtimeService = this.processEngine.getRuntimeService();
        this.taskService = this.processEngine.getTaskService();
    }
}
