package org.flowable.engine.delegate.event;

/**
 * An {@link org.flowable.engine.common.api.delegate.event.FlowableEvent} related to start event being sent when a process
 * instance is started.
 *
 * @author Christophe DENEUX - Linagora
 */
public interface FlowableProcessStartedEvent extends FlowableEntityWithVariablesEvent {

    /**
     * @return the id of the process instance of the nested process that starts the current process instance, or null if
     *         the current process instance is not started into a nested process.
     */
    String getNestedProcessInstanceId();

    /**
     * @return the id of the process definition of the nested process that starts the current process instance, or null
     *         if the current process instance is not started into a nested process.
     */
    String getNestedProcessDefinitionId();
}
