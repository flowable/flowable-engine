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
package org.flowable.engine.common.impl.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.AbstractEngineConfiguration;

/**
 * @author Tom Baeyens
 * @author Agim Emruli
 * @author Joram Barrez
 */
public class CommandContext extends AbstractCommandContext {

    protected Map<String, AbstractEngineConfiguration> engineConfigurations;

    public CommandContext(Command<?> command) {
        super(command);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
        if (closeListeners == null) {
            closeListeners = new ArrayList<BaseCommandContextCloseListener<AbstractCommandContext>>(1);
        }
        closeListeners.add((BaseCommandContextCloseListener) commandContextCloseListener);
    }

    public Map<String, AbstractEngineConfiguration> getEngineConfigurations() {
        return engineConfigurations;
    }

    public void setEngineConfigurations(Map<String, AbstractEngineConfiguration> engineConfigurations) {
        this.engineConfigurations = engineConfigurations;
    }
    
    public void addEngineConfiguration(String engineKey, AbstractEngineConfiguration engineConfiguration) {
        if (engineConfigurations == null) {
            engineConfigurations = new HashMap<>();
        }
        engineConfigurations.put(engineKey, engineConfiguration);
    }
    
}
