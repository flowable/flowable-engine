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
import org.flowable.common.engine.impl.javax.el.CouldNotResolvePropertyELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ListELResolver;
import org.flowable.common.engine.impl.javax.el.MapELResolver;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;

/**
 * Default {@link ExpressionManager} implementation that contains the logic for creating 
 * and resolving {@link Expression} instances. 
 *
 * @author Tom Baeyens
 * @author Dave Syer
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultExpressionManager implements ExpressionManager {

    protected ExpressionFactory expressionFactory;
    protected List<FlowableFunctionDelegate> functionDelegates;
    protected FlowableFunctionResolver functionResolver;
    protected FlowableFunctionResolverFactory functionResolverFactory = FunctionDelegatesFlowableFunctionResolver::new;
    protected List<FlowableAstFunctionCreator> astFunctionCreators;

    protected ELContext parsingElContext;
    protected Map<Object, Object> beans;
    
    protected DeploymentCache<Expression> expressionCache;
    protected int expressionTextLengthCacheLimit = -1;
    
    protected List<ELResolver> preDefaultResolvers;
    protected ELResolver jsonNodeResolver;
    protected List<ELResolver> postDefaultResolvers;
    protected List<ELResolver> preBeanResolvers;
    protected ELResolver beanResolver;

    protected ELResolver staticElResolver;

    public DefaultExpressionManager(Map<Object, Object> beans) {
        this.expressionFactory = ExpressionFactoryResolver.resolveExpressionFactory();
        this.beans = beans;
    }

    @Override
    public Expression createExpression(String text) {
        
        if (isCacheEnabled(text)) {
            Expression cachedExpression = expressionCache.get(text);
            if (cachedExpression != null) {
                return cachedExpression;
            }
        }
        
        if (parsingElContext == null) {
            this.parsingElContext = new ParsingElContext(functionResolver);
        } else if (parsingElContext.getFunctionMapper() != null && parsingElContext.getFunctionMapper() instanceof FlowableFunctionMapper) {
            ((FlowableFunctionMapper) parsingElContext.getFunctionMapper()).setFunctionResolver(functionResolver);
        }

        String expressionText = text.trim();
        
        ValueExpression valueExpression = expressionFactory.createValueExpression(parsingElContext, expressionText, Object.class);
        Expression expression = createJuelExpression(text, valueExpression);
        
        if (isCacheEnabled(text)) {
            expressionCache.add(text, expression);
        }
        
        return expression;
    }

    protected boolean isCacheEnabled(String text) {
        return expressionCache != null && (expressionTextLengthCacheLimit < 0 || text.length() <= expressionTextLengthCacheLimit);
    }

    protected Expression createJuelExpression(String expression, ValueExpression valueExpression) {
        return new JuelExpression(this, valueExpression, expression);
    }

    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }
    
    @Override
    public ELContext getElContext(VariableContainer variableContainer) {
        ELResolver elResolver = getOrCreateStaticElResolver();
        return new FlowableElContext(elResolver, functionResolver);
    }
    
    protected ELResolver getOrCreateStaticElResolver() {
        if (staticElResolver == null) {
            staticElResolver = new CompositeELResolver(createDefaultElResolvers());
        }

        return staticElResolver;
    }

    protected List<ELResolver> createDefaultElResolvers() {
        List<ELResolver> elResolvers = new ArrayList<>();
        elResolvers.add(createVariableElResolver());

        if (preDefaultResolvers != null) {
            elResolvers.addAll(preDefaultResolvers);
        }
        if (beans != null) {
            elResolvers.add(new ReadOnlyMapELResolver(beans));
        }
        elResolvers.add(new ArrayELResolver());
        elResolvers.add(new ListELResolver());
        elResolvers.add(new MapELResolver());
        ELResolver jsonNodeElResolver = createJsonNodeElResolver();
        if (jsonNodeElResolver != null) {
            elResolvers.add(jsonNodeElResolver);
        }
        if (preBeanResolvers != null) {
            elResolvers.addAll(preBeanResolvers);
        }

        ELResolver beanElResolver = createBeanElResolver();
        if (beanElResolver != null) {
            elResolvers.add(beanElResolver);
        }
        
        if (postDefaultResolvers != null) {
            elResolvers.addAll(postDefaultResolvers);
        }
    
        elResolvers.add(new CouldNotResolvePropertyELResolver());

        return elResolvers;
    }

    protected ELResolver createVariableElResolver() {
        return new VariableContainerELResolver();
    }

    protected ELResolver createJsonNodeElResolver() {
        return jsonNodeResolver == null ? new JsonNodeELResolver() : jsonNodeResolver;
    }
    
    protected ELResolver createBeanElResolver() {
        return beanResolver == null ? new BeanELResolver() : beanResolver;
    }

    @Override
    public Map<Object, Object> getBeans() {
        return beans;
    }

    @Override
    public void setBeans(Map<Object, Object> beans) {
        // When the beans are modified we need to reset the el resolver
        this.staticElResolver = null;
        this.beans = beans;
    }

    @Override
    public List<FlowableFunctionDelegate> getFunctionDelegates() {
        return functionDelegates;
    }

    @Override
    public void setFunctionDelegates(List<FlowableFunctionDelegate> functionDelegates) {
        this.functionDelegates = functionDelegates;

        updateFunctionResolver();
    }

    protected void updateFunctionResolver() {
        if (this.functionDelegates != null) {
            this.functionResolver = this.functionResolverFactory.create(this.functionDelegates);

        } else {
            this.functionResolver = null;

        }
    }
    
    @Override
    public List<FlowableAstFunctionCreator> getAstFunctionCreators() {
        return astFunctionCreators;
    }
    
    @Override
    public void setAstFunctionCreators(List<FlowableAstFunctionCreator> astFunctionCreators) {
        this.astFunctionCreators = astFunctionCreators;
        if (expressionFactory instanceof FlowableExpressionFactory) {
            ((FlowableExpressionFactory) expressionFactory).setAstFunctionCreators(astFunctionCreators);
        }
    }

    @Override
    public FlowableFunctionResolverFactory getFunctionResolverFactory() {
        return functionResolverFactory;
    }

    @Override
    public void setFunctionResolverFactory(FlowableFunctionResolverFactory functionResolverFactory) {
        this.functionResolverFactory = functionResolverFactory;
        updateFunctionResolver();
    }

    public DeploymentCache<Expression> getExpressionCache() {
        return expressionCache;
    }

    public void setExpressionCache(DeploymentCache<Expression> expressionCache) {
        this.expressionCache = expressionCache;
    }

    public int getExpressionTextLengthCacheLimit() {
        return expressionTextLengthCacheLimit;
    }

    public void setExpressionTextLengthCacheLimit(int expressionTextLengthCacheLimit) {
        this.expressionTextLengthCacheLimit = expressionTextLengthCacheLimit;
    }

    public void addPreDefaultResolver(ELResolver elResolver) {
        if (this.preDefaultResolvers == null) {
            this.preDefaultResolvers = new ArrayList<>();
        }

        this.preDefaultResolvers.add(elResolver);
    }

    public ELResolver getJsonNodeResolver() {
        return jsonNodeResolver;
    }

    public void setJsonNodeResolver(ELResolver jsonNodeResolver) {
        // When the bean resolver is modified we need to reset the el resolver
        this.staticElResolver = null;
        this.jsonNodeResolver = jsonNodeResolver;
    }

    public void addPostDefaultResolver(ELResolver elResolver) {
        if (this.postDefaultResolvers == null) {
            this.postDefaultResolvers = new ArrayList<>();
        }

        this.postDefaultResolvers.add(elResolver);
    }

    public void addPreBeanResolver(ELResolver elResolver) {
        if (this.preBeanResolvers == null) {
            this.preBeanResolvers = new ArrayList<>();
        }

        this.preBeanResolvers.add(elResolver);
    }

    public ELResolver getBeanResolver() {
        return beanResolver;
    }

    public void setBeanResolver(ELResolver beanResolver) {
        // When the bean resolver is modified we need to reset the el resolver
        this.staticElResolver = null;
        this.beanResolver = beanResolver;
    }
}
