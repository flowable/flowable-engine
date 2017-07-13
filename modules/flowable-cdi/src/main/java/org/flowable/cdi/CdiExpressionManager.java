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

import org.flowable.cdi.impl.el.CdiResolver;
import org.flowable.engine.common.impl.javax.el.ArrayELResolver;
import org.flowable.engine.common.impl.javax.el.BeanELResolver;
import org.flowable.engine.common.impl.javax.el.CompositeELResolver;
import org.flowable.engine.common.impl.javax.el.ELResolver;
import org.flowable.engine.common.impl.javax.el.ListELResolver;
import org.flowable.engine.common.impl.javax.el.MapELResolver;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.el.DefaultExpressionManager;
import org.flowable.engine.impl.el.VariableScopeElResolver;

/**
 * {@link DefaultExpressionManager} for resolving Cdi-managed beans.
 * 
 * This {@link DefaultExpressionManager} implementation performs lazy lookup of the Cdi-BeanManager and can thus be configured using the spring-based configuration of the process engine:
 * 
 * <pre>
 * &lt;property name="expressionManager"&gt;
 *      &lt;bean class="org.flowable.cdi.CdiExpressionManager" /&gt;
 * &lt;/property&gt;
 * </pre>
 * 
 * @author Daniel Meyer
 */
public class CdiExpressionManager extends DefaultExpressionManager {

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
