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

package org.flowable.engine.impl.event;

import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 */
public class CompensationEventHandler implements EventHandler {

    @Override
    public String getEventHandlerType() {
        return CompensateEventSubscriptionEntity.EVENT_TYPE;
    }

    @Override
    public void handleEvent(EventSubscriptionEntity eventSubscription, Object payload, CommandContext commandContext) {

        String configuration = eventSubscription.getConfiguration();
        if (configuration == null) {
            throw new FlowableException("Compensating execution not set for " + eventSubscription);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntity compensatingExecution = processEngineConfiguration.getExecutionEntityManager().findById(configuration);

        String processDefinitionId = compensatingExecution.getProcessDefinitionId();
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        if (process == null) {
            throw new FlowableException("Cannot start process instance. Process model (id = " + processDefinitionId + ") could not be found for " + eventSubscription);
        }

        FlowElement flowElement = process.getFlowElement(eventSubscription.getActivityId(), true);

        if (flowElement instanceof SubProcess && !((SubProcess) flowElement).isForCompensation()) {

            // descend into scope:
            compensatingExecution.setScope(true);
            List<CompensateEventSubscriptionEntity> eventsForThisScope = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                    .findCompensateEventSubscriptionsByExecutionId(compensatingExecution.getId());
            ScopeUtil.throwCompensationEvent(eventsForThisScope, compensatingExecution, false);

        } else {

            try {

                FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                            FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_COMPENSATE, flowElement.getId(), flowElement.getName(),
                                    compensatingExecution.getId(), compensatingExecution.getProcessInstanceId(), compensatingExecution.getProcessDefinitionId(), flowElement),
                            processEngineConfiguration.getEngineCfgKey());
                }
                
                Activity compensationActivity = null;
                Activity activity = (Activity) flowElement;
                if (!activity.isForCompensation() && activity.getBoundaryEvents().size() > 0) {
                    for (BoundaryEvent boundaryEvent : activity.getBoundaryEvents()) {
                        if (boundaryEvent.getEventDefinitions().size() > 0 && boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {
                            List<Association> associations = process.findAssociationsWithSourceRefRecursive(boundaryEvent.getId());
                            for (Association association : associations) {
                                FlowElement targetElement = process.getFlowElement(association.getTargetRef(), true);
                                if (targetElement instanceof Activity targetActivity) {
                                    if (targetActivity.isForCompensation()) {
                                        compensationActivity = targetActivity;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (compensationActivity != null) {
                    flowElement = compensationActivity;
                }
                
                compensatingExecution.setCurrentFlowElement(flowElement);
                CommandContextUtil.getAgenda().planContinueProcessInCompensation(compensatingExecution);

            } catch (Exception e) {
                throw new FlowableException("Error while handling compensation event " + eventSubscription, e);
            }

        }
    }

}
