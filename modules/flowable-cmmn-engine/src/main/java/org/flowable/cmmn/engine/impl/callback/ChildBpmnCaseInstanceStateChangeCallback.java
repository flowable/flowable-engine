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
package org.flowable.cmmn.engine.impl.callback;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback implementation for a child case instance (started from a process instance) returning it's state change to its parent.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ChildBpmnCaseInstanceStateChangeCallback implements RuntimeInstanceStateChangeCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChildBpmnCaseInstanceStateChangeCallback.class);

    @Override
    public void stateChanged(CallbackData callbackData) {
        
        /*
         * The child case instance has the execution id as callback id stored.
         * When the child case instance is finished, the execution of the parent process instance needs to be triggered.
         */
        
        if (CaseInstanceState.TERMINATED.equals(callbackData.getNewState())
                || CaseInstanceState.COMPLETED.equals(callbackData.getNewState())) {
            
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            CaseInstanceEntity caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(callbackData.getInstanceId());
            ProcessInstanceService processInstanceService = cmmnEngineConfiguration.getProcessInstanceService();
            
            Map<String, Object> variables = new HashMap<>();
            for (IOParameter outParameter : processInstanceService.getOutputParametersOfCaseTask(callbackData.getCallbackId())) {

                Object value = null;
                if (StringUtils.isNotEmpty(outParameter.getSourceExpression())) {
                    Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(outParameter.getSourceExpression().trim());
                    value = expression.getValue(caseInstance);

                } else {
                    value = caseInstance.getVariable(outParameter.getSource());
                }

                String variableName = null;
                if (StringUtils.isNotEmpty(outParameter.getTarget())) {
                    variableName = outParameter.getTarget();

                } else if (StringUtils.isNotEmpty(outParameter.getTargetExpression())) {
                    Object variableNameValue = cmmnEngineConfiguration.getExpressionManager().createExpression(outParameter.getTargetExpression()).getValue(caseInstance);
                    if (variableNameValue != null) {
                        variableName = variableNameValue.toString();
                    } else {
                        LOGGER.warn("Out parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                            outParameter.getTargetExpression());
                    }

                }

                variables.put(variableName, value);
            }
            
            processInstanceService.triggerCaseTask(callbackData.getCallbackId(), variables);
        }
    }

}
