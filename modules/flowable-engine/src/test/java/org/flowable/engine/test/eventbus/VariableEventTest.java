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

package org.flowable.engine.test.eventbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventbus.FlowableEventBusConsumer;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventbus.FlowableEventBusItemConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.service.impl.eventbus.FlowableEventBusVariableConstants;
import org.junit.jupiter.api.Test;

public class VariableEventTest extends PluggableFlowableTestCase implements FlowableEventBusVariableConstants, FlowableEventBusItemConstants {
    
    protected TestVariableEventConsumer variableEventConsumer = new TestVariableEventConsumer();

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testVariableCreateEvent() {
        runtimeService.addEventBusConsumer(variableEventConsumer);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            assertEquals(0, variableEventConsumer.getEvents().size());
            runtimeService.setVariable(processInstance.getId(), "testVariable", "test");
            assertEquals(1, variableEventConsumer.getEvents().size());
            FlowableEventBusEvent event = variableEventConsumer.getEvents().get(0);
            assertEquals(TYPE_VARIABLE_CREATED, event.getType());
            assertEquals(processInstance.getId(), event.getScopeId());
            assertEquals(ScopeTypes.BPMN, event.getScopeType());
            assertEquals("testVariable", event.getData().get(VARIABLE_NAME));
            assertEquals("test", event.getData().get(VARIABLE_VALUE));
            assertEquals("string", event.getData().get(VARIABLE_TYPE));
            assertNotNull(event.getData().get(VARIABLE_ID));
            assertEquals(processInstance.getId(), event.getData().get(PROCESS_INSTANCE_ID));
            assertEquals(processInstance.getId(), event.getData().get(EXECUTION_ID));
            assertNull(event.getData().get(TASK_ID));
            assertNull(event.getData().get(SCOPE_ID));
            assertNull(event.getData().get(SUB_SCOPE_ID));
            assertNull(event.getData().get(SCOPE_TYPE));
            
        } finally {
            variableEventConsumer.clearEvents();
            runtimeService.removeEventBusConsumer(variableEventConsumer);
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testVariableUpdateEvent() {
        runtimeService.addEventBusConsumer(variableEventConsumer);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            assertEquals(0, variableEventConsumer.getEvents().size());
            runtimeService.setVariable(processInstance.getId(), "testVariable", "test");
            assertEquals(1, variableEventConsumer.getEvents().size());
            
            runtimeService.setVariable(processInstance.getId(), "testVariable", "test2");
            assertEquals(2, variableEventConsumer.getEvents().size());
            
            FlowableEventBusEvent event = variableEventConsumer.getEvents().get(0);
            assertEquals(TYPE_VARIABLE_CREATED, event.getType());
            
            event = variableEventConsumer.getEvents().get(1);
            assertEquals(TYPE_VARIABLE_UPDATED, event.getType());
            assertEquals(processInstance.getId(), event.getScopeId());
            assertEquals(ScopeTypes.BPMN, event.getScopeType());
            
            assertEquals("testVariable", event.getData().get(VARIABLE_NAME));
            assertEquals("test2", event.getData().get(VARIABLE_VALUE));
            assertEquals("string", event.getData().get(VARIABLE_TYPE));
            assertEquals("test", event.getData().get(OLD_VARIABLE_VALUE));
            assertEquals("string", event.getData().get(OLD_VARIABLE_TYPE));
            assertNotNull(event.getData().get(VARIABLE_ID));
            assertEquals(processInstance.getId(), event.getData().get(PROCESS_INSTANCE_ID));
            assertEquals(processInstance.getId(), event.getData().get(EXECUTION_ID));
            assertNull(event.getData().get(TASK_ID));
            assertNull(event.getData().get(SCOPE_ID));
            assertNull(event.getData().get(SUB_SCOPE_ID));
            assertNull(event.getData().get(SCOPE_TYPE));
            
        } finally {
            variableEventConsumer.clearEvents();
            runtimeService.removeEventBusConsumer(variableEventConsumer);
        }
    }

    protected class TestVariableEventConsumer implements FlowableEventBusConsumer {
        
        protected List<FlowableEventBusEvent> events = new ArrayList<>();
        
        @Override
        public List<String> getSupportedTypes() {
            return Arrays.asList(TYPE_VARIABLE_CREATED, TYPE_VARIABLE_UPDATED);
        }

        @Override
        public void eventReceived(FlowableEventBusEvent event) {
            events.add(event);
        }
        
        public List<FlowableEventBusEvent> getEvents() {
            return events;
        }

        public void clearEvents() {
            events.clear();
        }
    }
}
