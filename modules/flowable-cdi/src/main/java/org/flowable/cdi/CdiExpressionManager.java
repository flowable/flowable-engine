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
import org.flowable.common.engine.impl.el.DefaultExpressionManager;
import org.flowable.engine.impl.el.ProcessExpressionManager;

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
 * @deprecated when not using the {@link CdiStandaloneProcessEngineConfiguration} or {@link CdiJtaProcessEngineConfiguration} add the {@link CdiResolver} as a custom resolver instead
 */
@Deprecated
public class CdiExpressionManager extends ProcessExpressionManager {

    public CdiExpressionManager() {
        addPreDefaultResolver(new CdiResolver());
    }

}
