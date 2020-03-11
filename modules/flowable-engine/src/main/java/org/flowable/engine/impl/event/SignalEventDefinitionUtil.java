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
public class SignalEventDefinitionUtil {

    /**
     * Determines the signal name of the {@link SignalEventDefinition} that is passed:
     * - if a signal ref is set, it has precedence
     * - if a signalExpression is set, it is returned
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

}
