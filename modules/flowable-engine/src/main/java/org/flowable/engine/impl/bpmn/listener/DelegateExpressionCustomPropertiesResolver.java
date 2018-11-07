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
package org.flowable.engine.impl.bpmn.listener;

import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.CustomPropertiesResolver;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * @author Yvo Swillens
 */
public class DelegateExpressionCustomPropertiesResolver implements CustomPropertiesResolver {

    protected Expression expression;

    public DelegateExpressionCustomPropertiesResolver(Expression expression) {
        this.expression = expression;
    }

    @Override
    public Map<String, Object> getCustomPropertiesMap(DelegateExecution execution) {
        // Note: we can't cache the result of the expression, because the
        // execution can change: eg.
        // delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
        Object delegate = expression.getValue(execution);

        if (delegate instanceof CustomPropertiesResolver) {
            return ((CustomPropertiesResolver) delegate).getCustomPropertiesMap(execution);
        } else {
            throw new FlowableIllegalArgumentException("Custom properties resolver delegate expression " + expression + " did not resolve to an implementation of " + CustomPropertiesResolver.class);
        }
    }

    /**
     * returns the expression text for this execution listener. Comes in handy if you want to check which listeners you already have.
     */
    public String getExpressionText() {
        return expression.getExpressionText();
    }

}
