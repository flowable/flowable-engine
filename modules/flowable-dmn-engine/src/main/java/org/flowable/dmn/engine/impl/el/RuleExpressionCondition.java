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

package org.flowable.dmn.engine.impl.el;

import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;

/**
 * Resolves an boolean EL expression at runtime.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class RuleExpressionCondition {

    protected Expression expression;

    public RuleExpressionCondition(Expression expression) {
        this.expression = expression;
    }

    public boolean evaluate(Map<String, Object> variables) {
        Object result = expression.getValue(new VariableContainerWrapper(variables));

        if (result == null) {
            throw new FlowableException("condition expression returns null");
        }
        if (!(result instanceof Boolean)) {
            throw new FlowableException("condition expression returns non-Boolean: " + result + " (" + result.getClass().getName() + ")");
        }
        return (Boolean) result;
    }

}
