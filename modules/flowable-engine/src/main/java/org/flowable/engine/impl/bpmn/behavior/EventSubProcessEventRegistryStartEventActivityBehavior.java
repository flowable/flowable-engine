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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventInstanceBpmnUtil;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * Implementation of the BPMN 2.0 event subprocess event registry start event.
 * 
 * @author Tijs Rademakers
 */
public class EventSubProcessEventRegistryStartEventActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String eventDefinitionKey;

    public EventSubProcessEventRegistryStartEventActivityBehavior(String eventDefinitionKey) {
        this.eventDefinitionKey = eventDefinitionKey;
    }

    @Override
    public void execute(DelegateExecution execution) {
        StartEvent startEvent = (StartEvent) execution.getCurrentFlowElement();
        EventSubProcess eventSubProcess = (EventSubProcess) startEvent.getSubProcess();

        execution.setScope(true);

        // initialize the template-defined data objects as variables
        Map<String, Object> dataObjectVars = processDataObjects(eventSubProcess.getDataObjects());
        if (dataObjectVars != null) {
            execution.setVariablesLocal(dataObjectVars);
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        StartEvent startEvent = (StartEvent) execution.getCurrentFlowElement();

        Object eventInstance = execution.getTransientVariable(EventConstants.EVENT_INSTANCE);
        if (eventInstance instanceof EventInstance) {
            EventInstanceBpmnUtil.handleEventInstanceOutParameters(execution, startEvent, (EventInstance) eventInstance);
        }

        if (startEvent.isInterrupting()) {
            List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(executionEntity.getParent());
            for (int i = childExecutions.size() - 1; i >= 0; i--) {
                ExecutionEntity childExecutionEntity = childExecutions.get(i);
                if (!childExecutionEntity.isEnded() && !childExecutionEntity.getId().equals(executionEntity.getId())) {
                    executionEntityManager.deleteExecutionAndRelatedData(childExecutionEntity,
                            DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(" + startEvent.getId() + ")", false);
                }
            }

            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();

            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (Objects.equals(eventDefinitionKey, eventSubscription.getEventType())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                    CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscription);
                }
            }
        }

        ExecutionEntity newSubProcessExecution = executionEntityManager.createChildExecution(executionEntity.getParent());
        newSubProcessExecution.setCurrentFlowElement((SubProcess) executionEntity.getCurrentFlowElement().getParentContainer());
        newSubProcessExecution.setEventScope(false);
        newSubProcessExecution.setScope(true);

        processEngineConfiguration.getActivityInstanceEntityManager().recordActivityStart(newSubProcessExecution);

        ExecutionEntity outgoingFlowExecution = executionEntityManager.createChildExecution(newSubProcessExecution);
        outgoingFlowExecution.setCurrentFlowElement(startEvent);

        processEngineConfiguration.getActivityInstanceEntityManager().recordActivityStart(outgoingFlowExecution);

        leave(outgoingFlowExecution);
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }
}
