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
package org.activiti.engine.impl.bpmn.helper;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

public class SkipExpressionUtil {

    public static boolean isSkipExpressionEnabled(DelegateExecution execution, Expression skipExpression) {

        if (skipExpression == null) {
            return false;
        }

        final String skipExpressionEnabledVariable = "_ACTIVITI_SKIP_EXPRESSION_ENABLED";
        Object isSkipExpressionEnabled = execution.getVariable(skipExpressionEnabledVariable);

        if (isSkipExpressionEnabled == null) {
            return false;

        } else if (isSkipExpressionEnabled instanceof Boolean) {
            return ((Boolean) isSkipExpressionEnabled).booleanValue();

        } else {
            throw new ActivitiIllegalArgumentException(skipExpressionEnabledVariable + " variable does not resolve to a boolean. " + isSkipExpressionEnabled);
        }
    }

    public static boolean shouldSkipFlowElement(DelegateExecution execution, Expression skipExpression) {
        Object value = skipExpression.getValue(execution);

        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();

        } else {
            throw new ActivitiIllegalArgumentException("Skip expression does not resolve to a boolean: " + skipExpression.getExpressionText());
        }
    }
}
