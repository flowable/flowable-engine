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

import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.FunctionMapper;
import org.flowable.common.engine.impl.javax.el.VariableMapper;

/**
 * Simple implementation of the {@link ELContext} used during parsings.
 * 
 * Currently this implementation does nothing, but a non-null implementation of the {@link ELContext} interface is required by the {@link ExpressionFactory} when create value- and methodexpressions.
 * 
 * @see ExpressionManager#createExpression(String)
 * @see ExpressionManager#createMethodExpression(String)
 * 
 * @author Joram Barrez
 */
public class ParsingElContext extends ELContext {

    protected List<FlowableFunctionDelegate> functionDelegates;

    public ParsingElContext(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;
    }

    @Override
    public ELResolver getELResolver() {
        return null;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return new FlowableFunctionMapper(functionDelegates);
    }

    @Override
    public VariableMapper getVariableMapper() {
        return null;
    }

}
