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

import javax.script.ScriptEngine;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.scripting.JSR223FlowableScriptEngine;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author Filip Hrisafov
 */
public class OsgiJSR223FlowableScriptEngine extends JSR223FlowableScriptEngine {

    @Override
    protected ScriptEngine getEngineByName(String language) {
        ScriptEngine scriptEngine = null;
        try {
            scriptEngine = Extender.resolveScriptEngine(language);
        } catch (InvalidSyntaxException e) {
            throw new FlowableException("problem resolving scripting engine: " + e.getMessage(), e);
        }

        if (scriptEngine == null) {
            return super.getEngineByName(language);
        }

        return scriptEngine;
    }
}
