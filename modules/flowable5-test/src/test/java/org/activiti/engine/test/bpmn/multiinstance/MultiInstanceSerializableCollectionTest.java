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
package org.activiti.engine.test.bpmn.multiinstance;

import java.util.ArrayList;
import java.util.HashMap;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;

/**
 * Multi instance tests where the collection is a Java collection that needs to be persisted as a process variable. The Flowable 5 engine has no other
 * variable type for Java collections and only supports a {@link java.util.Collection} as the multi instance collection, so these tests require
 * serializable variables to be enabled.
 */
public class MultiInstanceSerializableCollectionTest extends ResourceFlowableTestCase {

    public MultiInstanceSerializableCollectionTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    @Deployment(resources = "org/activiti/engine/test/bpmn/multiinstance/MultiInstanceTest.testSequentialCallActivityWithList.bpmn20.xml")
    public void testSequentialCallActivityWithList() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");

        HashMap<String, Object> variables = new HashMap<String, Object>();
        variables.put("list", list);

        String procId = runtimeService.startProcessInstanceByKey("parentProcess", variables).getId();

        org.flowable.task.api.Task task1 = taskService.createTaskQuery().processVariableValueEquals("element", "one").singleResult();
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().processVariableValueEquals("element", "two").singleResult();

        assertNotNull(task1);
        assertNotNull(task2);

        HashMap<String, Object> subVariables = new HashMap<String, Object>();
        subVariables.put("x", "y");

        taskService.complete(task1.getId(), subVariables);
        taskService.complete(task2.getId(), subVariables);

        org.flowable.task.api.Task task3 = taskService.createTaskQuery().processDefinitionKey("midProcess").singleResult();
        assertNotNull(task3);
        taskService.complete(task3.getId());

        org.flowable.task.api.Task task4 = taskService.createTaskQuery().processDefinitionKey("parentProcess").singleResult();
        assertNotNull(task4);
        taskService.complete(task4.getId());

        assertProcessEnded(procId);
    }

}
