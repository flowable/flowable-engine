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
package org.activiti.examples.variables;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Variable tests that rely on serializable variables, which are disabled by default.
 */
public class SerializableVariablesTest extends ResourceFlowableTestCase {

    public SerializableVariablesTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    // Test case for ACT-1839
    @Deployment(resources = { "org/activiti/examples/variables/VariablesTest.testChangeTypeSerializable.bpmn20.xml" })
    public void testChangeTypeSerializable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variable-type-change-test");
        assertNotNull(processInstance);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Activiti is awesome!", task.getName());
        SomeSerializable myVar = (SomeSerializable) runtimeService.getVariable(processInstance.getId(), "myVar");
        assertEquals("someValue", myVar.getValue());
    }

}
