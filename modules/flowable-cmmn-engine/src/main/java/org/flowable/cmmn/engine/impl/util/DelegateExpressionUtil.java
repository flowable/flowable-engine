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
package org.flowable.cmmn.engine.impl.util;

import java.util.List;

import org.flowable.cmmn.engine.impl.cfg.DelegateExpressionFieldInjectionMode;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.util.ReflectUtil;
import org.flowable.variable.service.delegate.VariableScope;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class DelegateExpressionUtil {

    public static Object resolveDelegateExpression(Expression expression,
            VariableScope variableScope, List<FieldExtension> fieldExtensions) {

        // Note: we can't cache the result of the expression, because the
        // execution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
        Object delegate = expression.getValue(variableScope);

        if (fieldExtensions != null && fieldExtensions.size() > 0) {

            DelegateExpressionFieldInjectionMode injectionMode = CommandContextUtil.getCmmnEngineConfiguration().getDelegateExpressionFieldInjectionMode();
            if (injectionMode == DelegateExpressionFieldInjectionMode.COMPATIBILITY) {
                applyFieldExtensions(fieldExtensions, delegate, variableScope, true);
            } else if (injectionMode == DelegateExpressionFieldInjectionMode.MIXED) {
                applyFieldExtensions(fieldExtensions, delegate, variableScope, false);
            }

        }

        return delegate;
    }
    
    protected static void applyFieldExtensions(List<FieldExtension> fieldExtensions, Object target, VariableScope variableScope, boolean throwExceptionOnMissingField) {
        if (fieldExtensions != null) {
            for (FieldExtension fieldExtension : fieldExtensions) {
                applyFieldExtension(fieldExtension, target, variableScope, throwExceptionOnMissingField);
            }
        }
    }

    protected static void applyFieldExtension(FieldExtension fieldExtension, Object target, VariableScope variableScope, boolean throwExceptionOnMissingField) {
        Object value = null;
        if (fieldExtension.getStringValue() != null) {
            value = fieldExtension.getStringValue();
        } else if (fieldExtension.getExpression() != null) {
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager();
            value = expressionManager.createExpression(fieldExtension.getExpression());
        }
        
        ReflectUtil.invokeSetterOrField(target, fieldExtension.getFieldName(), value, throwExceptionOnMissingField);
    }

}
