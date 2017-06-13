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
package org.flowable.bpm.model.bpmn.builder;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.EscalationEventDefinition;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormData;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;

public abstract class AbstractStartEventBuilder<B extends AbstractStartEventBuilder<B>>
        extends AbstractCatchEventBuilder<B, StartEvent> {

    protected AbstractStartEventBuilder(BpmnModelInstance modelInstance, StartEvent element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Sets the Flowable form handler class attribute.
     *
     * @param flowableFormHandlerClass the class name of the form handler
     * @return the builder object
     */
    public B flowableFormHandlerClass(String flowableFormHandlerClass) {
        element.setFlowableFormHandlerClass(flowableFormHandlerClass);
        return myself;
    }

    /**
     * Sets the Flowable form key attribute.
     *
     * @param flowableFormKey the form key to set
     * @return the builder object
     */
    public B flowableFormKey(String flowableFormKey) {
        element.setFlowableFormKey(flowableFormKey);
        return myself;
    }

    /**
     * Sets the Flowable initiator attribute.
     *
     * @param flowableInitiator the initiator to set
     * @return the builder object
     */
    public B flowableInitiator(String flowableInitiator) {
        element.setFlowableInitiator(flowableInitiator);
        return myself;
    }

    /**
     * Creates a new Flowable form field extension element.
     *
     * @return the builder object
     */
    public FlowableStartEventFormFieldBuilder flowableFormField() {
        FlowableFormData flowableFormData = getCreateSingleExtensionElement(FlowableFormData.class);
        FlowableFormField flowableFormField = createChild(flowableFormData, FlowableFormField.class);
        return new FlowableStartEventFormFieldBuilder(modelInstance, element, flowableFormField);
    }

    /**
     * Sets a catch all error definition.
     *
     * @return the builder object
     */
    public B error() {
        ErrorEventDefinition errorEventDefinition = createInstance(ErrorEventDefinition.class);
        element.getEventDefinitions().add(errorEventDefinition);

        return myself;
    }

    /**
     * Sets an error definition for the given error code. If already an error with this code exists it will be used, otherwise a new error is created.
     *
     * @param errorCode the code of the error
     * @return the builder object
     */
    public B error(String errorCode) {
        ErrorEventDefinition errorEventDefinition = createErrorEventDefinition(errorCode);
        element.getEventDefinitions().add(errorEventDefinition);

        return myself;
    }

    /**
     * Creates an error event definition with an unique id and returns a builder for the error event definition.
     *
     * @return the error event definition builder object
     */
    public ErrorEventDefinitionBuilder errorEventDefinition(String id) {
        ErrorEventDefinition errorEventDefinition = createEmptyErrorEventDefinition();
        if (id != null) {
            errorEventDefinition.setId(id);
        }

        element.getEventDefinitions().add(errorEventDefinition);
        return new ErrorEventDefinitionBuilder(modelInstance, errorEventDefinition);
    }

    /**
     * Creates an error event definition and returns a builder for the error event definition.
     *
     * @return the error event definition builder object
     */
    public ErrorEventDefinitionBuilder errorEventDefinition() {
        ErrorEventDefinition errorEventDefinition = createEmptyErrorEventDefinition();
        element.getEventDefinitions().add(errorEventDefinition);
        return new ErrorEventDefinitionBuilder(modelInstance, errorEventDefinition);
    }

    /**
     * Sets a catch all escalation definition.
     *
     * @return the builder object
     */
    public B escalation() {
        EscalationEventDefinition escalationEventDefinition = createInstance(EscalationEventDefinition.class);
        element.getEventDefinitions().add(escalationEventDefinition);

        return myself;
    }

    /**
     * Sets an escalation definition for the given escalation code. If already an escalation with this code exists it will be used, otherwise a new
     * escalation is created.
     *
     * @param escalationCode the code of the escalation
     * @return the builder object
     */
    public B escalation(String escalationCode) {
        EscalationEventDefinition escalationEventDefinition = createEscalationEventDefinition(escalationCode);
        element.getEventDefinitions().add(escalationEventDefinition);

        return myself;
    }

    /**
     * Sets a catch compensation definition.
     *
     * @return the builder object
     */
    public B compensation() {
        CompensateEventDefinition compensateEventDefinition = createCompensateEventDefinition();
        element.getEventDefinitions().add(compensateEventDefinition);

        return myself;
    }

    /**
     * Sets whether the start event is interrupting or not.
     */
    public B interrupting(boolean interrupting) {
        element.setInterrupting(interrupting);

        return myself;
    }

}
