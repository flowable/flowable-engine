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
package org.flowable.variable.service.event.impl;

import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableExceptionEvent;
import org.flowable.variable.service.event.FlowableVariableEvent;
import org.flowable.variable.service.event.FlowableVariableServiceEventType;
import org.flowable.variable.service.impl.types.VariableType;

/**
 * Builder class used to create {@link FlowableEvent} implementations.
 *
 * @author Frederik Heremans
 */
public class FlowableEventBuilder {

    /**
     * @param type
     *            type of event
     * @return an {@link FlowableEvent} that doesn't have it's execution context-fields filled, as the event is a global event, independent of any running execution.
     */
    public static FlowableEvent createGlobalEvent(FlowableVariableServiceEventType type) {
        FlowableEventImpl newEvent = new FlowableEventImpl(type);
        return newEvent;
    }

    public static FlowableEvent createEvent(FlowableVariableServiceEventType type, String executionId, String processInstanceId, String processDefinitionId) {
        FlowableEventImpl newEvent = new FlowableEventImpl(type);
        newEvent.setExecutionId(executionId);
        newEvent.setProcessDefinitionId(processDefinitionId);
        newEvent.setProcessInstanceId(processInstanceId);
        return newEvent;
    }

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @return an {@link FlowableEntityEvent}. In case an ExecutionContext is active, the execution related event fields will be populated. If not, execution details will be retrieved from the
     *         {@link Object} if possible.
     */
    public static FlowableEntityEvent createEntityEvent(FlowableVariableServiceEventType type, Object entity) {
        FlowableEntityEventImpl newEvent = new FlowableEntityEventImpl(entity, type);

        // In case an execution-context is active, populate the event fields
        // related to the execution
        //populateEventWithCurrentContext(newEvent);
        return newEvent;
    }

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @return an {@link FlowableEntityEvent}
     */
    public static FlowableEntityEvent createEntityEvent(FlowableVariableServiceEventType type, Object entity, String executionId, String processInstanceId, String processDefinitionId) {
        FlowableEntityEventImpl newEvent = new FlowableEntityEventImpl(entity, type);

        newEvent.setExecutionId(executionId);
        newEvent.setProcessInstanceId(processInstanceId);
        newEvent.setProcessDefinitionId(processDefinitionId);
        return newEvent;
    }

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @param cause
     *            the cause of the event
     * @return an {@link FlowableEntityEvent} that is also instance of {@link FlowableExceptionEvent}. In case an ExecutionContext is active, the execution related event fields will be
     *         populated.
     */
    public static FlowableEntityEvent createEntityExceptionEvent(FlowableVariableServiceEventType type, Object entity, Throwable cause) {
        FlowableEntityExceptionEventImpl newEvent = new FlowableEntityExceptionEventImpl(entity, type, cause);

        // In case an execution-context is active, populate the event fields
        // related to the execution
        //populateEventWithCurrentContext(newEvent);
        return newEvent;
    }

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @param cause
     *            the cause of the event
     * @return an {@link FlowableEntityEvent} that is also instance of {@link FlowableExceptionEvent}.
     */
    public static FlowableEntityEvent createEntityExceptionEvent(FlowableVariableServiceEventType type, Object entity, Throwable cause, String executionId, String processInstanceId, String processDefinitionId) {
        FlowableEntityExceptionEventImpl newEvent = new FlowableEntityExceptionEventImpl(entity, type, cause);

        newEvent.setExecutionId(executionId);
        newEvent.setProcessInstanceId(processInstanceId);
        newEvent.setProcessDefinitionId(processDefinitionId);
        return newEvent;
    }

    public static FlowableVariableEvent createVariableEvent(FlowableVariableServiceEventType type, String variableName, Object variableValue, VariableType variableType, String taskId, String executionId,
            String processInstanceId, String processDefinitionId) {
        FlowableVariableEventImpl newEvent = new FlowableVariableEventImpl(type);
        newEvent.setVariableName(variableName);
        newEvent.setVariableValue(variableValue);
        newEvent.setVariableType(variableType);
        newEvent.setTaskId(taskId);
        newEvent.setExecutionId(executionId);
        newEvent.setProcessDefinitionId(processDefinitionId);
        newEvent.setProcessInstanceId(processInstanceId);
        return newEvent;
    }
}
