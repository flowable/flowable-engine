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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.javax.el.ArrayELResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ListELResolver;
import org.flowable.common.engine.impl.javax.el.MapELResolver;
import org.flowable.common.engine.impl.javax.el.ValueExpression;

/**
 * Default {@link ExpressionManager} implementation that contains the logic for creating 
 * and resolving {@link Expression} instances. 
 *
 * @author Tom Baeyens
 * @author Dave Syer
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class DefaultExpressionManager implements ExpressionManager {

    protected ExpressionFactory expressionFactory;
    protected List<FlowableFunctionDelegate> functionDelegates;

    protected ELContext parsingElContext;
    protected Map<Object, Object> beans;

    public DefaultExpressionManager() {
        this(null);
    }

    public DefaultExpressionManager(Map<Object, Object> beans) {
        this.expressionFactory = ExpressionFactoryResolver.resolveExpressionFactory();
        this.beans = beans;
    }

    @Override
    public Expression createExpression(String expression) {
        if (parsingElContext == null) {
            this.parsingElContext = new ParsingElContext(functionDelegates);
        } else if (parsingElContext.getFunctionMapper() != null && parsingElContext.getFunctionMapper() instanceof FlowableFunctionMapper) {
            ((FlowableFunctionMapper) parsingElContext.getFunctionMapper()).setFunctionDelegates(functionDelegates);
        }

        ValueExpression valueExpression = expressionFactory.createValueExpression(parsingElContext, expression.trim(), Object.class);
        return createJuelExpression(expression, valueExpression);
    }

    protected Expression createJuelExpression(String expression, ValueExpression valueExpression) {
        return new JuelExpression(this, valueExpression, expression);
    }

    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }
    
    @Override
    public ELContext getElContext(VariableContainer variableContainer) {
        ELResolver elResolver = createElResolver(variableContainer);
        return new FlowableElContext(elResolver, functionDelegates);
    }
    
    protected ELResolver createElResolver(VariableContainer variableContainer) {
        List<ELResolver> elResolvers = new ArrayList<>();
        elResolvers.add(createVariableElResolver(variableContainer));
        if (beans != null) {
            elResolvers.add(new ReadOnlyMapELResolver(beans));
        }
        elResolvers.add(new ArrayELResolver());
        elResolvers.add(new ListELResolver());
        elResolvers.add(new MapELResolver());
        elResolvers.add(new JsonNodeELResolver());
        ELResolver beanElResolver = createBeanElResolver();
        if (beanElResolver != null) {
            elResolvers.add(beanElResolver);
        }
        
        configureResolvers(elResolvers);
        
        CompositeELResolver compositeELResolver = new CompositeELResolver();
        for (ELResolver elResolver : elResolvers) {
            compositeELResolver.add(elResolver);
        }
        return compositeELResolver;
    }
    
    protected void configureResolvers(List<ELResolver> elResolvers) {
        // to be extended if needed
    }

    protected ELResolver createVariableElResolver(VariableContainer variableContainer) {
        return new VariableContainerELResolver(variableContainer);
    }
    
    protected ELResolver createBeanElResolver() {
        return new BeanELResolver();
    }

    @Override
    public Map<Object, Object> getBeans() {
        return beans;
    }

    @Override
    public void setBeans(Map<Object, Object> beans) {
        this.beans = beans;
    }

    @Override
    public List<FlowableFunctionDelegate> getFunctionDelegates() {
        return functionDelegates;
    }

    @Override
    public void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;
    }
}
