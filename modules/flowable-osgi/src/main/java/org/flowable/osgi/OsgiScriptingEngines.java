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
import javax.script.ScriptException;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.variable.api.delegate.VariableScope;
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
    public Object evaluate(String script, String language, VariableScope variableScope) {
        Bindings bindings = createBindings(variableScope);
        return evaluate(script, language, bindings);
    }

    @Override
    public Object evaluate(String script, String language, VariableScope variableScope, boolean storeScriptVariables) {
        return evaluate(script, language, createBindings(variableScope, storeScriptVariables));
    }

    @Override
    protected Object evaluate(String script, String language, Bindings bindings) {
        ScriptEngine scriptEngine = null;
        try {
            scriptEngine = Extender.resolveScriptEngine(language);
        } catch (InvalidSyntaxException e) {
            throw new FlowableException("problem resolving scripting engine: " + e.getMessage(), e);
        }

        if (scriptEngine == null) {
            return super.evaluate(script, language, bindings);
        }

        try {
            return scriptEngine.eval(script, bindings);
        } catch (ScriptException e) {
            throw new FlowableException("problem evaluating script: " + e.getMessage(), e);
        }
    }
}
