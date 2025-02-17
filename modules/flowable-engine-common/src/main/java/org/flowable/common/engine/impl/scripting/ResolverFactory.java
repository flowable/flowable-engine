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


import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface ResolverFactory {

    /**
     * Create a {@link Resolver} instance.
     *
     * @param engineConfiguration the engine configuration
     * @param scopeContainer the scope variable container in which the script is executed
     * @param inputVariableContainer the variable container that should be used to resolve variables
     * @return the created {@link Resolver} instance
     */
    Resolver createResolver(AbstractEngineConfiguration engineConfiguration, VariableContainer scopeContainer, VariableContainer inputVariableContainer);

}
