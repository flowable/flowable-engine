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

import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 */
public class BoundaryCompensateEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected CompensateEventDefinition compensateEventDefinition;

    public BoundaryCompensateEventActivityBehavior(CompensateEventDefinition compensateEventDefinition, boolean interrupting) {
        super(interrupting);
        this.compensateEventDefinition = compensateEventDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
        if (process == null) {
            throw new FlowableException("Process model for " + executionEntity + " could not be found");
        }

        Activity sourceActivity = null;
        Activity compensationActivity = null;
        List<Association> associations = process.findAssociationsWithSourceRefRecursive(boundaryEvent.getId());
        for (Association association : associations) {
            sourceActivity = boundaryEvent.getAttachedToRef();
            FlowElement targetElement = process.getFlowElement(association.getTargetRef(), true);
            if (targetElement instanceof Activity activity) {
                if (activity.isForCompensation()) {
                    compensationActivity = activity;
                    break;
                }
            }
        }
        
        if (sourceActivity == null) {
            throw new FlowableException("Parent activity for boundary compensation event could not be found for " + executionEntity);
        }

        if (compensationActivity == null) {
            throw new FlowableException("Compensation activity could not be found (or it is missing 'isForCompensation=\"true\"') for " + executionEntity);
        }

        // find SubProcess or Process instance execution
        ExecutionEntity scopeExecution = null;
        ExecutionEntity parentExecution = executionEntity.getParent();
        while (scopeExecution == null && parentExecution != null) {
            if (parentExecution.getCurrentFlowElement() instanceof SubProcess) {
                scopeExecution = parentExecution;

            } else if (parentExecution.isProcessInstanceType()) {
                scopeExecution = parentExecution;
            } else {
                parentExecution = parentExecution.getParent();
            }
        }

        if (scopeExecution == null) {
            throw new FlowableException("Could not find a scope execution for compensation boundary event " + boundaryEvent.getId() + " for " + executionEntity);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) processEngineConfiguration.getEventSubscriptionServiceConfiguration()
                .getEventSubscriptionService().createEventSubscriptionBuilder()
                        .eventType(CompensateEventSubscriptionEntity.EVENT_TYPE)
                        .executionId(scopeExecution.getId())
                        .processInstanceId(scopeExecution.getProcessInstanceId())
                        .activityId(sourceActivity.getId())
                        .tenantId(scopeExecution.getTenantId())
                        .create();
        
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        if (boundaryEvent.isCancelActivity()) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof CompensateEventSubscriptionEntity && eventSubscription.getActivityId().equals(compensateEventDefinition.getActivityRef())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                    CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscription);
                }
            }
        }

        super.trigger(executionEntity, triggerName, triggerData);
    }
}
