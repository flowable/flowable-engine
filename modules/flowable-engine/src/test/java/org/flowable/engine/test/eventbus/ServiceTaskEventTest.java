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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.eventbus.FlowableEventBusItem;
import org.flowable.common.engine.api.eventbus.FlowableEventConsumer;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.eventbus.FlowableEventBusItemConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.eventbus.FlowableEventBusBpmnConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ServiceTaskEventTest extends PluggableFlowableTestCase implements FlowableEventBusBpmnConstants, FlowableEventBusItemConstants {
    
    protected TestServiceTaskEventConsumer serviceTaskEventConsumer = new TestServiceTaskEventConsumer();

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/serviceTask.bpmn20.xml")
    public void testVariableCreateEvent() {
        runtimeService.addEventConsumer(serviceTaskEventConsumer);
        try {
            assertEquals(0, serviceTaskEventConsumer.getEvents().size());
            
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("startToEnd").latestVersion().singleResult();
            
            try {
                runtimeService.startProcessInstanceByKey("startToEnd");
                fail("expected exception");
                
            } catch (Exception e) {
                // expected
            }
            
            assertEquals(1, serviceTaskEventConsumer.getEvents().size());
            FlowableEventBusItem event = serviceTaskEventConsumer.getEvents().get(0);
            assertEquals(TYPE_SERVICETASK_EXCEPTION, event.getType());
            assertNotNull(event.getScopeId());
            assertEquals(ScopeTypes.BPMN, event.getScopeType());
            assertEquals(processDefinition.getId(), event.getScopeDefinitionId());
            assertEquals(processDefinition.getKey(), event.getScopeDefinitionKey());
            assertEquals("task", event.getData().get(FLOW_ELEMENT_ID));
            assertEquals("Test task", event.getData().get(FLOW_ELEMENT_NAME));
            assertEquals("ServiceTask", event.getData().get(FLOW_ELEMENT_TYPE));
            assertNotNull(event.getData().get(PROCESS_INSTANCE_ID));
            assertNotNull(event.getData().get(EXECUTION_ID));
            assertEquals(processDefinition.getId(), event.getData().get(PROCESS_DEFINITION_ID));
            
        } finally {
            serviceTaskEventConsumer.clearEvents();
            runtimeService.removeEventConsumer(serviceTaskEventConsumer);
        }
    }

    public static class ExceptionServiceTaskDelegate implements JavaDelegate {
        
        public ExceptionServiceTaskDelegate() {}
        
        @Override
        public void execute(DelegateExecution execution) {
            throw new FlowableException("Test exception");
        }
    }
    
    protected class TestServiceTaskEventConsumer implements FlowableEventConsumer {
        
        protected List<FlowableEventBusItem> events = new ArrayList<>();
        
        @Override
        public List<String> getSupportedTypes() {
            return Arrays.asList(TYPE_SERVICETASK_EXCEPTION);
        }

        @Override
        public void eventReceived(FlowableEventBusItem event) {
            events.add(event);
        }
        
        public List<FlowableEventBusItem> getEvents() {
            return events;
        }

        public void clearEvents() {
            events.clear();
        }
    }
}
