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
package org.flowable.common.engine.impl.el;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.javax.el.ELContext;

/**
 * Used as an entry point for runtime evaluation of the expressions.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface ExpressionManager {

    Expression createExpression(String expression);
    
    ELContext getElContext(VariableContainer variableContainer);
    
    Map<Object, Object> getBeans();
    
    void setBeans(Map<Object, Object> beans);
    
    List<FlowableFunctionDelegate> getFunctionDelegates();
    
    void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates);

}