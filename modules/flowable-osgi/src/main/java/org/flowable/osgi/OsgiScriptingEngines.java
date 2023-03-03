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
package org.flowable.osgi;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptEvaluation;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author Tijs Rademakers
 */
public class OsgiScriptingEngines extends ScriptingEngines {

    public OsgiScriptingEngines(ScriptBindingsFactory scriptBindingsFactory) {
        super(scriptBindingsFactory);
    }

    public OsgiScriptingEngines(ScriptEngineManager scriptEngineManager) {
        super(scriptEngineManager);
    }

    @Override
    public ScriptEvaluation evaluate(ScriptEngineRequest request) {
        return super.evaluate(request);
    }

    @Override
    public Object evaluate(String script, String language, VariableContainer variableContainer) {
        return super.evaluate(script, language, variableContainer);
    }

    @Override
    public Object evaluate(String script, String language, VariableContainer variableContainer, boolean storeScriptVariables) {
        return super.evaluate(script, language, variableContainer, storeScriptVariables);
    }

    @Override
    protected Object evaluate(ScriptEngineRequest request, Bindings bindings) {
        ScriptEngine scriptEngine = null;
        try {
            scriptEngine = Extender.resolveScriptEngine(request.getLanguage());
        } catch (InvalidSyntaxException e) {
            throw new FlowableException("problem resolving scripting engine: " + e.getMessage(), e);
        }

        if (scriptEngine == null) {
            return super.evaluate(request, bindings);
        }
        return evaluate(scriptEngine, request, bindings);
    }
}
