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
package org.flowable.osgi.blueprint;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.common.engine.impl.javax.el.ArrayELResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.CouldNotResolvePropertyELResolver;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.ListELResolver;
import org.flowable.common.engine.impl.javax.el.MapELResolver;
import org.flowable.common.engine.impl.scripting.BeansResolverFactory;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.flowable.engine.impl.el.ProcessExpressionManager;
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
            compositeElResolver.add(new CouldNotResolvePropertyELResolver());
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
