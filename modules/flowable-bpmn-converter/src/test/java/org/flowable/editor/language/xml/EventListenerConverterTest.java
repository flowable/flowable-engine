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
package org.flowable.editor.language.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.junit.Test;

/**
 * Test for ACT-1657
 * 
 * @author Frederik Heremans
 */
public class EventListenerConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "eventlistenersmodel.bpmn20.xml";
    }

    private void validateModel(BpmnModel model) {
        Process process = model.getMainProcess();
        assertNotNull(process);
        assertNotNull(process.getEventListeners());
        assertEquals(8, process.getEventListeners().size());

        // Listener with class
        EventListener listener = process.getEventListeners().get(0);
        assertEquals("ENTITY_CREATE", listener.getEvents());
        assertEquals("org.activiti.test.MyListener", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());

        // Listener with class, but no specific event (== all events)
        listener = process.getEventListeners().get(1);
        assertNull(listener.getEvents());
        assertEquals("org.activiti.test.AllEventTypesListener", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, listener.getImplementationType());

        // Listener with delegate expression
        listener = process.getEventListeners().get(2);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("${myListener}", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, listener.getImplementationType());

        // Listener that throws a signal-event
        listener = process.getEventListeners().get(3);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("theSignal", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_THROW_SIGNAL_EVENT, listener.getImplementationType());

        // Listener that throws a global signal-event
        listener = process.getEventListeners().get(4);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("theSignal", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_THROW_GLOBAL_SIGNAL_EVENT, listener.getImplementationType());

        // Listener that throws a message-event
        listener = process.getEventListeners().get(5);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("theMessage", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_THROW_MESSAGE_EVENT, listener.getImplementationType());

        // Listener that throws an error-event
        listener = process.getEventListeners().get(6);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("123", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT, listener.getImplementationType());

        // Listener restricted to a specific entity
        listener = process.getEventListeners().get(7);
        assertEquals("ENTITY_DELETE", listener.getEvents());
        assertEquals("123", listener.getImplementation());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT, listener.getImplementationType());
        assertEquals("job", listener.getEntityType());
    }
}
