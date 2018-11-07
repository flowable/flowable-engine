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

package org.flowable.engine.impl.el;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Expression that always returns the same value when <code>getValue</code> is called. Setting of the value is not supported.
 * 
 * @author Frederik Heremans
 */
public class FixedValue implements Expression {

    private static final long serialVersionUID = 1L;
    private Object value;

    public FixedValue(Object value) {
        this.value = value;
    }
    
    @Override
    public Object getValue(VariableContainer variableContainer) {
        return value;
    }
    
    @Override
    public void setValue(Object value, VariableContainer variableContainer) {
        throw new FlowableException("Cannot change fixed value");
    }

    @Override
    public String getExpressionText() {
        return value.toString();
    }

}
