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
package org.flowable.engine.impl.util;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.event.EventHandler;
import org.flowable.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSubscriptionUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionUtil.class);

    public static void eventReceived(EventSubscriptionEntity eventSubscriptionEntity, Object payload, boolean processAsync) {
        if (processAsync) {
            scheduleEventAsync(eventSubscriptionEntity, payload);
        } else {
            processEventSync(eventSubscriptionEntity, payload);
        }
    }
    
    public static void processPayloadMap(Object payload, ExecutionEntity execution, FlowNode currentFlowElement, CommandContext commandContext) {
        if (payload instanceof Map) {
            
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) payload;
            if (currentFlowElement instanceof Event) {
                Event event = (Event) currentFlowElement;
                if (event.getInParameters().size() > 0) {
                    
                    VariableContainerWrapper variableWrapper = new VariableContainerWrapper(payloadMap);
                    ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager();
                    for (IOParameter inParameter : event.getInParameters()) {

                        Object value = null;
                        if (StringUtils.isNotEmpty(inParameter.getSourceExpression())) {
                            Expression expression = expressionManager.createExpression(inParameter.getSourceExpression().trim());
                            value = expression.getValue(variableWrapper);

                        } else {
                            value = variableWrapper.getVariable(inParameter.getSource());
                        }

                        String variableName = null;
                        if (StringUtils.isNotEmpty(inParameter.getTargetExpression())) {
                            Expression expression = expressionManager.createExpression(inParameter.getTargetExpression());
                            Object variableNameValue = expression.getValue(variableWrapper);
                            if (variableNameValue != null) {
                                variableName = variableNameValue.toString();
                            } else {
                                LOGGER.warn("In parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                                    inParameter.getTargetExpression());
                            }

                        } else if (StringUtils.isNotEmpty(inParameter.getTarget())){
                            variableName = inParameter.getTarget();

                        }
                        
                        execution.setVariable(variableName, value);
                    }
                    
                } else {
                    execution.setVariables(payloadMap);
                }
                
            } else {
                execution.setVariables(payloadMap);
            }
        }
    }

    protected static void processEventSync(EventSubscriptionEntity eventSubscriptionEntity, Object payload) {
        // A compensate event needs to be deleted before the handlers are called
        if (eventSubscriptionEntity instanceof CompensateEventSubscriptionEntity) {
            CommandContextUtil.getEventSubscriptionService().deleteEventSubscription(eventSubscriptionEntity);
            CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscriptionEntity);
        }

        EventHandler eventHandler = CommandContextUtil.getProcessEngineConfiguration().getEventHandler(eventSubscriptionEntity.getEventType());
        if (eventHandler == null) {
            throw new FlowableException("Could not find eventhandler for event of type '" + eventSubscriptionEntity.getEventType() + "'.");
        }
        eventHandler.handleEvent(eventSubscriptionEntity, payload, CommandContextUtil.getCommandContext());
    }

    protected static void scheduleEventAsync(EventSubscriptionEntity eventSubscriptionEntity, Object payload) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        JobService jobService = CommandContextUtil.getJobService(commandContext);
        JobEntity message = jobService.createJob();
        message.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        message.setJobHandlerType(ProcessEventJobHandler.TYPE);
        message.setElementId(eventSubscriptionEntity.getActivityId());
        message.setJobHandlerConfiguration(eventSubscriptionEntity.getId());
        message.setTenantId(eventSubscriptionEntity.getTenantId());
        
        String executionId = eventSubscriptionEntity.getExecutionId();
        
        if (StringUtils.isNotEmpty(executionId)) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
            FlowNode currentFlowElement = (FlowNode) execution.getCurrentFlowElement();
    
            if (currentFlowElement == null) {
                throw new FlowableException("Error while sending signal for event subscription '" + eventSubscriptionEntity.getId() + "': " + "no activity associated with event subscription");
            }
            
            EventSubscriptionUtil.processPayloadMap(payload, execution, currentFlowElement, commandContext);
        }

        jobService.scheduleAsyncJob(message);
    }
}
