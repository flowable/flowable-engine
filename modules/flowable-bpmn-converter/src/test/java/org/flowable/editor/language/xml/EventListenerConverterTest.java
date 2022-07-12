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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventListener;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

/**
 * Test for ACT-1657
 * 
 * @author Frederik Heremans
 */
class EventListenerConverterTest {

    @BpmnXmlConverterTest("eventlistenersmodel.bpmn20.xml")
    void validateModel(BpmnModel model) {
        Process process = model.getMainProcess();
        assertThat(process).isNotNull();
        assertThat(process.getEventListeners()).isNotNull();
        assertThat(process.getEventListeners())
                .extracting(EventListener::getEvents, EventListener::getImplementation, EventListener::getImplementationType, EventListener::getEntityType)
                .containsExactly(
                        // Listener with class
                        tuple("ENTITY_CREATE", "org.activiti.test.MyListener", ImplementationType.IMPLEMENTATION_TYPE_CLASS, null),
                        // Listener with class, but no specific event (== all events)
                        tuple(null, "org.activiti.test.AllEventTypesListener", ImplementationType.IMPLEMENTATION_TYPE_CLASS, null),
                        // Listener with delegate expression
                        tuple("ENTITY_DELETE", "${myListener}", ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, null),
                        // Listener that throws a signal-event
                        tuple("ENTITY_DELETE", "theSignal", ImplementationType.IMPLEMENTATION_TYPE_THROW_SIGNAL_EVENT, null),
                        // Listener that throws a global signal-event
                        tuple("ENTITY_DELETE", "theSignal", ImplementationType.IMPLEMENTATION_TYPE_THROW_GLOBAL_SIGNAL_EVENT, null),
                        // Listener that throws a message-event
                        tuple("ENTITY_DELETE", "theMessage", ImplementationType.IMPLEMENTATION_TYPE_THROW_MESSAGE_EVENT, null),
                        // Listener that throws an error-event
                        tuple("ENTITY_DELETE", "123", ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT, null),
                        // Listener restricted to a specific entity
                        tuple("ENTITY_DELETE", "123", ImplementationType.IMPLEMENTATION_TYPE_THROW_ERROR_EVENT, "job")
                );
    }

    @BpmnXmlConverterTest(value = "eventlistenerscript.bpmn20.xml")
    void validateEventListenerScriptParsing(BpmnModel model) {
        Process process = model.getMainProcess();
        UserTask userTask = (UserTask) process.getFlowElement("userTask");
        assertThat(userTask).isNotNull();
        assertThat(userTask.getTaskListeners()).hasSize(2);

        List<FlowableListener> taskListeners = userTask.getTaskListeners();

        // Verify custom ScriptTaskListener
        FlowableListener scriptTaskListenerClass = taskListeners.get(0);

        assertThat(scriptTaskListenerClass.getEvent()).isEqualTo("create");
        assertThat(scriptTaskListenerClass.getImplementationType()).isEqualTo("class");
        assertThat(scriptTaskListenerClass.getImplementation()).isEqualTo("org.flowable.engine.impl.bpmn.listener.ScriptTaskListener");
        assertThat(scriptTaskListenerClass.getFieldExtensions())
                .extracting(FieldExtension::getFieldName)
                .containsExactly("script", "language", "resultVariable");

        // Verify new type=script taskListener.
        FlowableListener scriptTaskListenerType = taskListeners.get(1);
        assertThat(scriptTaskListenerType.getEvent()).isEqualTo("create");
        assertThat(scriptTaskListenerType.getImplementationType()).isEqualTo("script");
        assertThat(scriptTaskListenerType.getImplementation()).isNull();
        assertThat(scriptTaskListenerType.getScriptInfo()).isNotNull();
        assertThat(scriptTaskListenerType.getScriptInfo().getScript()).contains(" task.setVariable('scriptTaskListenerType', \"Type\");");
        assertThat(scriptTaskListenerType.getScriptInfo().getLanguage()).isEqualTo("groovy");
        assertThat(scriptTaskListenerType.getScriptInfo().getResultVariable()).isEqualTo("scriptTypeResult");
    }
}
