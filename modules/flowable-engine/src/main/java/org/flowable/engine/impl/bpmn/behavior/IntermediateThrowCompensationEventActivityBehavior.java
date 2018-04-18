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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ThrowEvent;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IntermediateThrowCompensationEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected final CompensateEventDefinition compensateEventDefinition;

    public IntermediateThrowCompensationEventActivityBehavior(CompensateEventDefinition compensateEventDefinition) {
        this.compensateEventDefinition = compensateEventDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ThrowEvent throwEvent = (ThrowEvent) execution.getCurrentFlowElement();

        /*
         * From the BPMN 2.0 spec:
         * 
         * The Activity to be compensated MAY be supplied.
         * 
         * If an Activity is not supplied, then the compensation is broadcast to all completed Activities in the current Sub- Process (if present), or the entire Process instance (if at the global
         * level). This “throws” the compensation.
         */
        final String activityRef = compensateEventDefinition.getActivityRef();

        CommandContext commandContext = Context.getCommandContext();
        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);

        List<CompensateEventSubscriptionEntity> eventSubscriptions = new ArrayList<>();
        if (StringUtils.isNotEmpty(activityRef)) {

            // If an activity ref is provided, only that activity is compensated
            eventSubscriptions.addAll(eventSubscriptionEntityManager
                    .findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(execution.getProcessInstanceId(), activityRef));

        } else {

            // If no activity ref is provided, it is broadcast to the current sub process / process instance
            Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());

            FlowElementsContainer flowElementsContainer = null;
            if (throwEvent.getSubProcess() == null) {
                flowElementsContainer = process;
            } else {
                flowElementsContainer = throwEvent.getSubProcess();
            }

            for (FlowElement flowElement : flowElementsContainer.getFlowElements()) {
                if (flowElement instanceof Activity) {
                    eventSubscriptions.addAll(eventSubscriptionEntityManager
                            .findCompensateEventSubscriptionsByProcessInstanceIdAndActivityId(execution.getProcessInstanceId(), flowElement.getId()));
                }
            }

        }

        if (eventSubscriptions.isEmpty()) {
            leave(execution);
        } else {
            // TODO: implement async (waitForCompletion=false in bpmn)
            ScopeUtil.throwCompensationEvent(eventSubscriptions, execution, false);
            leave(execution);
        }
    }
}
