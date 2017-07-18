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

import org.flowable.engine.common.api.delegate.FlowableFunctionDelegate;
import org.flowable.engine.common.impl.el.DynamicBeanPropertyELResolver;
import org.flowable.engine.common.impl.el.ExpressionFactoryResolver;
import org.flowable.engine.common.impl.el.FlowableElContext;
import org.flowable.engine.common.impl.el.JsonNodeELResolver;
import org.flowable.engine.common.impl.el.ParsingElContext;
import org.flowable.engine.common.impl.el.ReadOnlyMapELResolver;
import org.flowable.engine.common.impl.javax.el.ArrayELResolver;
import org.flowable.engine.common.impl.javax.el.BeanELResolver;
import org.flowable.engine.common.impl.javax.el.CompositeELResolver;
import org.flowable.engine.common.impl.javax.el.ELContext;
import org.flowable.engine.common.impl.javax.el.ELResolver;
import org.flowable.engine.common.impl.javax.el.ExpressionFactory;
import org.flowable.engine.common.impl.javax.el.ListELResolver;
import org.flowable.engine.common.impl.javax.el.MapELResolver;
import org.flowable.engine.common.impl.javax.el.ValueExpression;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.bpmn.data.ItemInstance;
import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.flowable.engine.impl.interceptor.DelegateInterceptor;
import org.flowable.engine.impl.persistence.entity.VariableScopeImpl;

/**
 * <p>
 * Central manager for all expressions.
 * </p>
 * <p>
 * Process parsers will use this to build expression objects that are stored in the process definitions.
 * </p>
 * <p>
 * Then also this class is used as an entry point for runtime evaluation of the expressions.
 * </p>
 *
 * @author Tom Baeyens
 * @author Dave Syer
 * @author Frederik Heremans
 */
public class DefaultExpressionManager implements ExpressionManager {

    protected DelegateInterceptor delegateInterceptor;
    protected ExpressionFactory expressionFactory;
    protected List<FlowableFunctionDelegate> functionDelegates;

    // Default implementation (does nothing)
    protected ELContext parsingElContext;
    protected Map<Object, Object> beans;

    public DefaultExpressionManager() {
        this(null);
    }

    public DefaultExpressionManager(boolean initFactory) {
        this(null, false);
    }

    public DefaultExpressionManager(Map<Object, Object> beans) {
        this(beans, true);
    }

    public DefaultExpressionManager(Map<Object, Object> beans, boolean initFactory) {
        this(new DefaultDelegateInterceptor(), beans, initFactory);
    }
    
    public DefaultExpressionManager(DelegateInterceptor delegateInterceptor, Map<Object, Object> beans, boolean initFactory) {
        this.delegateInterceptor = delegateInterceptor;
        this.expressionFactory = ExpressionFactoryResolver.resolveExpressionFactory();
        this.beans = beans;
    }

    /* (non-Javadoc)
     * @see org.flowable.engine.impl.el.ExpressionManager#createExpression(java.lang.String)
     */
    @Override
    public Expression createExpression(String expression) {
        if (parsingElContext == null) {
            this.parsingElContext = new ParsingElContext(functionDelegates);
        }

        ValueExpression valueExpression = expressionFactory.createValueExpression(parsingElContext, expression.trim(), Object.class);
        return new JuelExpression(this, this.delegateInterceptor, valueExpression, expression);
    }

    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    public ELContext getElContext(VariableScope variableScope) {
        ELContext elContext = null;
        if (variableScope instanceof VariableScopeImpl) {
            VariableScopeImpl variableScopeImpl = (VariableScopeImpl) variableScope;
            elContext = variableScopeImpl.getCachedElContext();
        }

        if (elContext == null) {
            elContext = createElContext(variableScope);
            if (variableScope instanceof VariableScopeImpl) {
                ((VariableScopeImpl) variableScope).setCachedElContext(elContext);
            }
        }

        return elContext;
    }

    protected FlowableElContext createElContext(VariableScope variableScope) {
        ELResolver elResolver = createElResolver(variableScope);
        return new FlowableElContext(elResolver, functionDelegates);
    }

    protected ELResolver createElResolver(VariableScope variableScope) {
        CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new VariableScopeElResolver(variableScope));

        if (beans != null) {
            // ACT-1102: Also expose all beans in configuration when using
            // standalone flowable, not in spring-context
            elResolver.add(new ReadOnlyMapELResolver(beans));
        }

        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new MapELResolver());
        elResolver.add(new JsonNodeELResolver());
        elResolver.add(new DynamicBeanPropertyELResolver(ItemInstance.class, "getFieldValue", "setFieldValue"));
        elResolver.add(new BeanELResolver());
        return elResolver;
    }

    public Map<Object, Object> getBeans() {
        return beans;
    }

    public void setBeans(Map<Object, Object> beans) {
        this.beans = beans;
    }

    public List<FlowableFunctionDelegate> getFunctionDelegates() {
        return functionDelegates;
    }

    public void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;
    }
}
