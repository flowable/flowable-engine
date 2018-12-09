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

/**
 * @author Tijs Rademakers
 */
public class ChildBpmnCaseInstanceStateChangeCallback implements RuntimeInstanceStateChangeCallback {

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
            for (IOParameter ioParameter : processInstanceService.getOutputParametersOfCaseTask(callbackData.getCallbackId())) {
                Object value = null;
                if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                    Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(ioParameter.getSourceExpression().trim());
                    value = expression.getValue(caseInstance);

                } else {
                    value = caseInstance.getVariable(ioParameter.getSource());
                }

                variables.put(ioParameter.getTarget(), value);
            }
            
            processInstanceService.triggerCaseTask(callbackData.getCallbackId(), variables);
        }
    }

}
