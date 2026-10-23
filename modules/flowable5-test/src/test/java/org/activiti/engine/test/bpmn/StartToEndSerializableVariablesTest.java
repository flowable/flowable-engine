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
package org.activiti.engine.test.bpmn;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.compatibility.wrapper.Flowable5ProcessInstanceWrapper;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Start to end tests with serializable variables, which need to be enabled explicitly.
 *
 * @author Joram Barrez
 */
public class StartToEndSerializableVariablesTest extends ResourceFlowableTestCase {

    public StartToEndSerializableVariablesTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    @Deployment(resources = { "org/activiti/engine/test/bpmn/StartToEndTest.testStartWithSerializableVariables.bpmn20.xml" })
    public void testStartProcessInstanceWithSerializbleVariables() {
        Map<String, Object> varMap = new HashMap<String, Object>();
        varMap.put("test", "hello");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd", varMap);
        assertProcessEnded(processInstance.getId());
        Map<String, Object> returnVarMap = ((ExecutionEntity) ((Flowable5ProcessInstanceWrapper) processInstance).getRawObject()).getVariables();
        assertEquals("hello", returnVarMap.get("test"));
        Person person1 = (Person) returnVarMap.get("person1");
        assertEquals("1", person1.getId());
        assertEquals("John", person1.getName());
        Person person2 = (Person) returnVarMap.get("person2");
        assertEquals("2", person2.getId());
        assertEquals("Paul", person2.getName());
    }

    public static class SerializableServiceTaskDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("person1", new Person("1", "John"));
            execution.setVariable("person2", new Person("2", "Paul"));
        }

    }

    public static class Person implements Serializable {
        private static final long serialVersionUID = 1L;

        protected String id;
        protected String name;

        public Person(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
