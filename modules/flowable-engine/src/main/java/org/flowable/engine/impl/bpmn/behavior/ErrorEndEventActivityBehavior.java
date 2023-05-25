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
import java.util.Map;

import org.flowable.bpmn.model.IOParameter;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IOParameterUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ErrorEndEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String errorCode;
    protected List<IOParameter> outParameters;

    public ErrorEndEventActivityBehavior(String errorCode, List<IOParameter> outParameters) {
        this.errorCode = errorCode;
        this.outParameters = outParameters;
    }

    @Override
    public void execute(DelegateExecution execution) {
        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
        Map<String, Object> payload = IOParameterUtil.extractOutVariables(outParameters, execution, expressionManager);
        Object errorMessage = payload.remove("errorMessage");
        BpmnError error = new BpmnError(errorCode, errorMessage != null ? errorMessage.toString() : null);
        if (!payload.isEmpty()) {
            error.setAdditionalDataContainer(new VariableContainerWrapper(payload));
        }
        ErrorPropagation.propagateError(error, execution);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

}
