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
import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
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

    /**
     * Creates an {@link Expression} instance from the given String.
     * Expression are resolved against a {@link VariableContainer} (e.g. a process Execution, a case instance plan item, etc.)
     */
    Expression createExpression(String expression);
    
    /**
     * Creates an {@link ELContext} against which {@link Expression} instance can be resolved.
     */
    ELContext getElContext(VariableContainer variableContainer);
    
    /**
     * Returns the beans registered with this expression manager instance.
     */
    Map<Object, Object> getBeans();
    
    /**
     * Sets the beans which can be used in expressions.
     */
    void setBeans(Map<Object, Object> beans);
    
    /**
     * Returns the custom functions registered and usable in expressions.
     */
    List<FlowableFunctionDelegate> getFunctionDelegates();
    
    /**
     * Set the custom functions usable in expressions. 
     */
    void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates);
    
    /**
     * Returns the {@link FlowableExpressionEnhancer} which potentially can alter the expression text 
     * before being transformed into an {@link Expression} instance.
     */
    List<FlowableExpressionEnhancer> getExpressionEnhancers();
    
    /**
     * Sets the {@link FlowableExpressionEnhancer} instances which can enhance expression texts 
     * before being tranformed into an {@link Expression} instance.
     */
    void setExpressionEnhancers(List<FlowableExpressionEnhancer> expressionEnhancers);

}