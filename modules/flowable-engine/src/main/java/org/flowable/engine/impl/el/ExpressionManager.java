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

import java.util.List;
import java.util.Map;

import javax.el.ELContext;

import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.FlowableFunctionDelegate;
import org.flowable.engine.delegate.VariableScope;

/**
 * Used as an entry point for runtime evaluation of the expressions.
 *
 * @author Tijs Rademakers
 */
public interface ExpressionManager {

    Expression createExpression(String expression);
    
    ELContext getElContext(VariableScope variableScope);
    
    Map<Object, Object> getBeans();
    
    void setBeans(Map<Object, Object> beans);
    
    List<FlowableFunctionDelegate> getFunctionDelegates();
    
    void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates);

}