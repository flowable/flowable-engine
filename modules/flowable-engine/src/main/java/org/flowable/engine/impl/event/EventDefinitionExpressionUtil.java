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

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;

/**
 * @author Joram Barrez
 */
public class EventDefinitionExpressionUtil {

    /**
     * Determines the signal name of the {@link SignalEventDefinition} that is passed:
     * - if a signal name is set, it has precedence
     * - otherwise, the signal ref is used
     * - unless a signalExpression is set
     */
    public static String determineSignalName(CommandContext commandContext, SignalEventDefinition signalEventDefinition, BpmnModel bpmnModel, DelegateExecution execution) {
        String signalName = null;
        if (StringUtils.isNotEmpty(signalEventDefinition.getSignalRef())) {
            Signal signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
            if (signal != null) {
                signalName = signal.getName();
            } else {
                signalName = signalEventDefinition.getSignalRef();
            }

        } else {
            signalName = signalEventDefinition.getSignalExpression();

        }

        if (StringUtils.isNotEmpty(signalName)) {
            Expression expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(signalName);
            return expression.getValue(execution != null ? execution : NoExecutionVariableScope.getSharedInstance()).toString();
        }

        return signalName;
    }

    /**
     * Determines the event name of the {@link org.flowable.bpmn.model.MessageEventDefinition} that is passed:
     * - if a message ref is set, it has precedence
     * - if a messageExpression is set, it is returned
     *
     * Note that, contrary to the determineSignalName method, the name of the message is never used.
     * This is because of historical reasons (and it can't be changed now without breaking existing models/instances)
     */
    public static String determineMessageName(CommandContext commandContext, MessageEventDefinition messageEventDefinition, DelegateExecution execution) {
        String messageName = null;
        if (StringUtils.isNotEmpty(messageEventDefinition.getMessageRef())) {
            return messageEventDefinition.getMessageRef();

        } else {
            messageName = messageEventDefinition.getMessageExpression();

        }

        if (StringUtils.isNotEmpty(messageName)) {
            Expression expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(messageName);
            return expression.getValue(execution != null ? execution : NoExecutionVariableScope.getSharedInstance()).toString();
        }

        return messageName;
    }

}
