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
package org.flowable.crystalball.simulator.impl;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.SimulationRunContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class drives simulation event handling by script.
 * 
 * @author martin.grofcik
 */
public class ScriptEventHandler implements SimulationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptEventHandler.class);

    protected String scriptPropertyName;
    protected String language;

    public ScriptEventHandler(String scriptPropertyName, String language) {
        this.scriptPropertyName = scriptPropertyName;
        this.language = language;
    }

    @Override
    public void init() {

    }

    @Override
    public void handle(SimulationEvent event) {
        ScriptingEngines scriptingEngines = CommandContextUtil.getProcessEngineConfiguration().getScriptingEngines();

        VariableScope execution = SimulationRunContext.getExecution();
        try {
            scriptingEngines.evaluate((String) event.getProperty(this.scriptPropertyName), language, execution, false);

        } catch (FlowableException e) {
            LOGGER.warn("Exception while executing simulation event {} scriptPropertyName :{}\n script: {}\n exception is:{}", event, this.scriptPropertyName, event.getProperty(this.scriptPropertyName), e.getMessage());
            throw e;
        }
    }
}
