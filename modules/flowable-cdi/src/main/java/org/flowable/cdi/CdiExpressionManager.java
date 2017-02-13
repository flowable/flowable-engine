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
package org.flowable.cdi;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELResolver;
import javax.el.ListELResolver;
import javax.el.MapELResolver;

import org.flowable.cdi.impl.el.CdiResolver;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.ExpressionManager;
import org.flowable.engine.impl.el.VariableScopeElResolver;

/**
 * {@link ExpressionManager} for resolving Cdi-managed beans.
 * 
 * This {@link ExpressionManager} implementation performs lazy lookup of the Cdi-BeanManager and can thus be configured using the spring-based configuration of the process engine:
 * 
 * <pre>
 * &lt;property name="expressionManager"&gt;
 *      &lt;bean class="org.flowable.cdi.CdiExpressionManager" /&gt;
 * &lt;/property&gt;
 * </pre>
 * 
 * @author Daniel Meyer
 */
public class CdiExpressionManager extends ExpressionManager {
  
  public CdiExpressionManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(processEngineConfiguration);
  }

  @Override
  protected ELResolver createElResolver(VariableScope variableScope) {
    CompositeELResolver compositeElResolver = new CompositeELResolver();
    compositeElResolver.add(new VariableScopeElResolver(variableScope));

    compositeElResolver.add(new CdiResolver());

    compositeElResolver.add(new ArrayELResolver());
    compositeElResolver.add(new ListELResolver());
    compositeElResolver.add(new MapELResolver());
    compositeElResolver.add(new BeanELResolver());
    return compositeElResolver;
  }

}
