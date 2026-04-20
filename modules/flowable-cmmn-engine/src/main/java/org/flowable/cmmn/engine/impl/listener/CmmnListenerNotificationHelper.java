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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class CmmnListenerNotificationHelper {

    public void executeTaskListeners(TaskEntity taskEntity, String eventType) {
        if (taskEntity.getScopeDefinitionId() != null) {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(taskEntity.getScopeDefinitionId());
            CaseElement caseElement = cmmnModel.getPrimaryCase().getAllCaseElements().get(taskEntity.getTaskDefinitionKey());
            if (caseElement instanceof HumanTask humanTask) {
                executeTaskListeners(humanTask, taskEntity, eventType);
            }
        }
    }

    public void executeTaskListeners(HumanTask humanTask, TaskEntity taskEntity, String eventType) {
        for (FlowableListener listener : humanTask.getTaskListeners()) {
            String event = listener.getEvent();
            if (event.equals(eventType) || event.equals(TaskListener.EVENTNAME_ALL_EVENTS)) {
                TaskListener taskListener = createTaskListener(listener);

                taskEntity.setEventName(eventType);
                taskEntity.setEventHandlerId(listener.getId());

                try {
                    taskListener.notify(taskEntity);
                } finally {
                    taskEntity.setEventName(null);
                }
            }
        }
    }

    protected TaskListener createTaskListener(FlowableListener listener) {
        TaskListener taskListener = null;

        CmmnListenerFactory listenerFactory = CommandContextUtil.getCmmnEngineConfiguration().getListenerFactory();
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(listener.getImplementationType())) {
            taskListener = listenerFactory.createClassDelegateTaskListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            taskListener = listenerFactory.createExpressionTaskListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            taskListener = listenerFactory.createDelegateExpressionTaskListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_INSTANCE.equalsIgnoreCase(listener.getImplementationType())) {
            taskListener = (TaskListener) listener.getInstance();
        } else if (ImplementationType.IMPLEMENTATION_TYPE_SCRIPT.equalsIgnoreCase(listener.getImplementationType())) {
            taskListener = listenerFactory.createScriptTypeTaskListener(listener);
        }
        return taskListener;
    }

    protected PlanItemInstanceLifecycleListener createLifecycleListener(FlowableListener listener) {
        PlanItemInstanceLifecycleListener lifecycleListener = null;

        CmmnListenerFactory listenerFactory = CommandContextUtil.getCmmnEngineConfiguration().getListenerFactory();
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createClassDelegateLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createExpressionLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createDelegateExpressionLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_INSTANCE.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = (PlanItemInstanceLifecycleListener) listener.getInstance();
        }

        return lifecycleListener;
    }

    public void executeLifecycleListeners(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        if (Objects.equals(oldState, newState)) {
            return;
        }

        // Lifecycle listeners on the element itself
        PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition != null) {
            List<FlowableListener> flowableListeners = planItemDefinition.getLifecycleListeners();
            if (flowableListeners != null && !flowableListeners.isEmpty()) {

                for (FlowableListener flowableListener : flowableListeners) {
                    if (stateMatches(flowableListener.getSourceState(), oldState) && stateMatches(flowableListener.getTargetState(), newState)) {
                        PlanItemInstanceLifecycleListener lifecycleListener = createLifecycleListener(flowableListener);
                        executeLifecycleListener(planItemInstance, oldState, newState, lifecycleListener, flowableListener);
                    }
                }
            }
        }

        // Lifecycle listeners defined on the cmmn engine configuration
        Map<String, List<PlanItemInstanceLifecycleListener>> planItemInstanceLifeCycleListeners
            = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getPlanItemInstanceLifecycleListeners();
        if (planItemInstanceLifeCycleListeners != null && !planItemInstanceLifeCycleListeners.isEmpty()) {

            List<PlanItemInstanceLifecycleListener> specificListeners = planItemInstanceLifeCycleListeners.get(planItemInstance.getPlanItemDefinitionType());
            executeListeners(specificListeners, planItemInstance, oldState, newState);

            List<PlanItemInstanceLifecycleListener> genericListeners = planItemInstanceLifeCycleListeners.get(null);
            executeListeners(genericListeners, planItemInstance, oldState, newState);

        }
    }

    public void executeListeners(List<PlanItemInstanceLifecycleListener> listeners, DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        if (listeners != null) {
            for (PlanItemInstanceLifecycleListener listener : listeners) {
                executeLifecycleListener(planItemInstance, oldState, newState, listener, null);
            }
        }
    }

    public void executeLifecycleListener(DelegatePlanItemInstance planItemInstance, String oldState, String newState,
            PlanItemInstanceLifecycleListener lifecycleListener, FlowableListener flowableListener) {
        if (lifecycleListenerMatches(lifecycleListener, oldState, newState)) {
            planItemInstance.setCurrentLifecycleListener(lifecycleListener, flowableListener);
            lifecycleListener.stateChanged(planItemInstance, oldState, newState);
            planItemInstance.setCurrentLifecycleListener(null, null);
        }
    }

    protected boolean lifecycleListenerMatches(PlanItemInstanceLifecycleListener lifecycleListener, String oldState, String newState) {
        return stateMatches(lifecycleListener.getSourceState(), oldState) && stateMatches(lifecycleListener.getTargetState(), newState);
    }

    protected boolean stateMatches(String listenerExpectedState, String actualState) {
        return StringUtils.isEmpty(listenerExpectedState) || Objects.equals(actualState, listenerExpectedState);
    }

    protected CaseInstanceLifecycleListener createCaseLifecycleListener(FlowableListener listener) {
        CaseInstanceLifecycleListener lifecycleListener = null;

        CmmnListenerFactory listenerFactory = CommandContextUtil.getCmmnEngineConfiguration().getListenerFactory();
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createClassDelegateCaseLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createExpressionCaseLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = listenerFactory.createDelegateExpressionCaseLifeCycleListener(listener);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_INSTANCE.equalsIgnoreCase(listener.getImplementationType())) {
            lifecycleListener = (CaseInstanceLifecycleListener) listener.getInstance();
        }

        return lifecycleListener;
    }

}
