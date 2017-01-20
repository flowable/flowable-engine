package org.activiti.engine.delegate.event;

/**
 * An {@link org.activiti.engine.common.api.delegate.event.ActivitiEvent} related to start event being sent when activiti process
 * instance is started.
 *
 * @author Christophe DENEUX - Linagora
 */
public interface ActivitiProcessStartedEvent extends ActivitiEntityWithVariablesEvent {

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
