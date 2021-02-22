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

package org.flowable.engine.impl.bpmn.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ScopeUtil {

    /**
     * we create a separate execution for each compensation handler invocation.
     */
    public static void throwCompensationEvent(List<CompensateEventSubscriptionEntity> eventSubscriptions, DelegateExecution execution, boolean async) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

        // first spawn the compensating executions
        for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
            ExecutionEntity compensatingExecution = null;

            // check whether compensating execution is already created (which is the case when compensating an embedded subprocess,
            // where the compensating execution is created when leaving the subprocess and holds snapshot data).
            if (eventSubscription.getConfiguration() != null) {
                compensatingExecution = executionEntityManager.findById(eventSubscription.getConfiguration());
                compensatingExecution.setParent(compensatingExecution.getProcessInstance());
                compensatingExecution.setEventScope(false);
            } else {
                compensatingExecution = executionEntityManager.createChildExecution((ExecutionEntity) execution);
                eventSubscription.setConfiguration(compensatingExecution.getId());
            }

        }

        // signal compensation events in reverse order of their 'created' timestamp
        eventSubscriptions.sort(new Comparator<EventSubscriptionEntity>() {

            @Override
            public int compare(EventSubscriptionEntity o1, EventSubscriptionEntity o2) {
                return o2.getCreated().compareTo(o1.getCreated());
            }
        });

        for (CompensateEventSubscriptionEntity compensateEventSubscriptionEntity : eventSubscriptions) {
            EventSubscriptionUtil.eventReceived(compensateEventSubscriptionEntity, null, async);
        }
    }

    /**
     * Creates a new event scope execution and moves existing event subscriptions to this new execution
     */
    public static void createCopyOfSubProcessExecutionForCompensation(ExecutionEntity subProcessExecution) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsByExecutionAndType(subProcessExecution.getId(), "compensate");

        List<CompensateEventSubscriptionEntity> compensateEventSubscriptions = new ArrayList<>();
        for (EventSubscriptionEntity event : eventSubscriptions) {
            if (event instanceof CompensateEventSubscriptionEntity) {
                compensateEventSubscriptions.add((CompensateEventSubscriptionEntity) event);
            }
        }

        if (CollectionUtil.isNotEmpty(compensateEventSubscriptions)) {

            ExecutionEntity processInstanceExecutionEntity = subProcessExecution.getProcessInstance();

            ExecutionEntity eventScopeExecution = processEngineConfiguration.getExecutionEntityManager().createChildExecution(processInstanceExecutionEntity);
            eventScopeExecution.setActive(false);
            eventScopeExecution.setEventScope(true);
            eventScopeExecution.setCurrentFlowElement(subProcessExecution.getCurrentFlowElement());

            // copy local variables to eventScopeExecution by value. This way,
            // the eventScopeExecution references a 'snapshot' of the local variables
            Map<String, Object> variables = subProcessExecution.getVariablesLocal();
            for (Entry<String, Object> variable : variables.entrySet()) {
                eventScopeExecution.setVariableLocal(variable.getKey(), variable.getValue(), subProcessExecution, true);
            }

            // set event subscriptions to the event scope execution:
            for (CompensateEventSubscriptionEntity eventSubscriptionEntity : compensateEventSubscriptions) {
                eventSubscriptionService.deleteEventSubscription(eventSubscriptionEntity);
                CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscriptionEntity);

                EventSubscriptionEntity newSubscription = (EventSubscriptionEntity) eventSubscriptionService.createEventSubscriptionBuilder()
                                .eventType(CompensateEventSubscriptionEntity.EVENT_TYPE)
                                .executionId(eventScopeExecution.getId())
                                .processInstanceId(eventScopeExecution.getProcessInstanceId())
                                .activityId(eventSubscriptionEntity.getActivityId())
                                .tenantId(eventScopeExecution.getTenantId())
                                .create();
                
                CountingEntityUtil.handleInsertEventSubscriptionEntityCount(newSubscription);
                
                newSubscription.setConfiguration(eventSubscriptionEntity.getConfiguration());
                newSubscription.setCreated(eventSubscriptionEntity.getCreated());
            }

            EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) eventSubscriptionService.createEventSubscriptionBuilder()
                            .eventType(CompensateEventSubscriptionEntity.EVENT_TYPE)
                            .executionId(processInstanceExecutionEntity.getId())
                            .processInstanceId(processInstanceExecutionEntity.getProcessInstanceId())
                            .activityId(eventScopeExecution.getCurrentFlowElement().getId())
                            .tenantId(processInstanceExecutionEntity.getTenantId())
                            .create();
            
            CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
            
            eventSubscription.setConfiguration(eventScopeExecution.getId());
        }
    }
}
