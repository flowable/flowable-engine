package org.flowable.osgi.blueprint;

import java.util.ArrayList;
import java.util.List;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELResolver;
import javax.el.ListELResolver;
import javax.el.MapELResolver;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.ExpressionManager;
import org.flowable.engine.impl.el.VariableScopeElResolver;
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
    configImpl.setExpressionManager(new BlueprintExpressionManager(configImpl));

    List<ResolverFactory> resolverFactories = configImpl.getResolverFactories();
    if (resolverFactories == null) {
      resolverFactories = new ArrayList<ResolverFactory>();
      resolverFactories.add(new VariableScopeResolverFactory());
      resolverFactories.add(new BeansResolverFactory());
    }

    configImpl.setScriptingEngines(new OsgiScriptingEngines(new ScriptBindingsFactory(configImpl, resolverFactories)));
    super.init();
  }

  public class BlueprintExpressionManager extends ExpressionManager {
    
    public BlueprintExpressionManager(ProcessEngineConfiguration processEngineConfiguration) {
      super(processEngineConfiguration);
    }
    
    @Override
    protected ELResolver createElResolver(VariableScope variableScope) {
      CompositeELResolver compositeElResolver = new CompositeELResolver();
      compositeElResolver.add(new VariableScopeElResolver(variableScope));
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
