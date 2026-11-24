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
package org.activiti.engine.test.api.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;

/**
 * Task variable tests for serializable variables, which need to be enabled explicitly.
 *
 * @author Tom Baeyens
 */
public class TaskSerializableVariableTest extends ResourceFlowableTestCase {

    public TaskSerializableVariableTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    public void testSerializableTaskVariable() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("MyTask");
        taskService.saveTask(task);

        // Set variable
        Map<String, Object> vars = new HashMap<String, Object>();
        MyVariable myVariable = new MyVariable("Hello world");
        vars.put("theVar", myVariable);
        taskService.setVariables(task.getId(), vars);

        // Fetch variable
        MyVariable variable = (MyVariable) taskService.getVariable(task.getId(), "theVar");
        assertEquals("Hello world", variable.getValue());

        // Cleanup
        taskService.deleteTask(task.getId(), true);
    }

    public static class MyVariable implements Serializable {

        private String value;

        public MyVariable(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
