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

package org.flowable.common.engine.impl.scripting;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Tom Baeyens
 */
public class BeansResolverFactory implements ResolverFactory, Resolver {

    protected AbstractEngineConfiguration engineConfiguration;

    @Override
    public Resolver createResolver(AbstractEngineConfiguration processEngineConfiguration, VariableScope variableScope) {
        this.engineConfiguration = processEngineConfiguration;
        return this;
    }

    @Override
    public boolean containsKey(Object key) {
        return engineConfiguration.getBeans().containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return engineConfiguration.getBeans().get(key);
    }
}
