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
package org.flowable.engine.impl.el;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class FlowableElContext extends ELContext {

    protected ELResolver elResolver;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public FlowableElContext(ELResolver elResolver, ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.elResolver = elResolver;
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public ELResolver getELResolver() {
        return elResolver;
    }

    public FunctionMapper getFunctionMapper() {
        return new FlowableFunctionMapper(processEngineConfiguration);
    }

    public VariableMapper getVariableMapper() {
        return null;
    }
}
