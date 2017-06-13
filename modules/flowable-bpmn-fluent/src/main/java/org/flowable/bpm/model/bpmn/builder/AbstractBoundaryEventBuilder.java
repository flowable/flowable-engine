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
import org.flowable.bpm.model.bpmn.instance.BoundaryEvent;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.EscalationEventDefinition;

public abstract class AbstractBoundaryEventBuilder<B extends AbstractBoundaryEventBuilder<B>>
        extends AbstractCatchEventBuilder<B, BoundaryEvent> {

    protected AbstractBoundaryEventBuilder(BpmnModelInstance modelInstance, BoundaryEvent element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    /**
     * Set if the boundary event cancels the attached activity.
     *
     * @param cancelActivity true if the boundary event cancels the activity, false otherwise
     * @return the builder object
     */
    public B cancelActivity(Boolean cancelActivity) {
        element.setCancelActivity(cancelActivity);

        return myself;
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
}
