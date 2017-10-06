package org.flowable.osgi.blueprint;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.common.api.variable.VariableContainer;
import org.flowable.engine.common.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.engine.common.impl.javax.el.ArrayELResolver;
import org.flowable.engine.common.impl.javax.el.BeanELResolver;
import org.flowable.engine.common.impl.javax.el.CompositeELResolver;
import org.flowable.engine.common.impl.javax.el.ELResolver;
import org.flowable.engine.common.impl.javax.el.ListELResolver;
import org.flowable.engine.common.impl.javax.el.MapELResolver;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.impl.scripting.BeansResolverFactory;
import org.flowable.engine.impl.scripting.ResolverFactory;
import org.flowable.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.engine.impl.scripting.VariableScopeResolverFactory;
import org.flowable.osgi.OsgiScriptingEngines;

public class ProcessEngineFactoryWithELResolver extends ProcessEngineFactory {

    private BlueprintELResolver blueprintELResolver;
    private BlueprintContextELResolver blueprintContextELResolver;

    @Override
    public void init() throws Exception {
        ProcessEngineConfigurationImpl configImpl = (ProcessEngineConfigurationImpl) getProcessEngineConfiguration();
        configImpl.setExpressionManager(new BlueprintExpressionManager());

        List<ResolverFactory> resolverFactories = configImpl.getResolverFactories();
        if (resolverFactories == null) {
            resolverFactories = new ArrayList<>();
            resolverFactories.add(new VariableScopeResolverFactory());
            resolverFactories.add(new BeansResolverFactory());
        }

        configImpl.setScriptingEngines(new OsgiScriptingEngines(new ScriptBindingsFactory(configImpl, resolverFactories)));
        super.init();
    }

    public class BlueprintExpressionManager extends ProcessExpressionManager {

        public BlueprintExpressionManager() {
            this.delegateInterceptor = new DefaultDelegateInterceptor();
            this.expressionFactory = new ExpressionFactoryImpl();
        }

        @Override
        protected ELResolver createElResolver(VariableContainer variableContainer) {
            CompositeELResolver compositeElResolver = new CompositeELResolver();
            compositeElResolver.add(createVariableElResolver(variableContainer));
            if (blueprintContextELResolver != null) {
                compositeElResolver.add(blueprintContextELResolver);
            }
            compositeElResolver.add(blueprintELResolver);
            compositeElResolver.add(new BeanELResolver());
            compositeElResolver.add(new ArrayELResolver());
            compositeElResolver.add(new ListELResolver());
            compositeElResolver.add(new MapELResolver());
            return compositeElResolver;
        }

    }

    public void setBlueprintELResolver(BlueprintELResolver blueprintELResolver) {
        this.blueprintELResolver = blueprintELResolver;
    }

    public void setBlueprintContextELResolver(BlueprintContextELResolver blueprintContextELResolver) {
        this.blueprintContextELResolver = blueprintContextELResolver;
    }
}
