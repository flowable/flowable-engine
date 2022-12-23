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
package org.flowable.scripting.secure.listener;

import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.impl.bpmn.listener.ScriptExecutionListener;
import org.flowable.scripting.secure.behavior.SecureJavascriptTaskParseHandler;
import org.flowable.scripting.secure.impl.SecureJavascriptUtil;

/**
 * @author Joram Barrez
 */
public class SecureJavascriptExecutionListener extends ScriptExecutionListener {

    @Override
    protected Object evaluateScript(ScriptingEngines engines, ScriptEngineRequest request) {
        if (SecureJavascriptTaskParseHandler.LANGUAGE_JAVASCRIPT.equalsIgnoreCase(request.getLanguage())) {
            return SecureJavascriptUtil.evaluateScript(request.getVariableContainer(), request.getScript());
        } else {
            return super.evaluateScript(engines, request);
        }
    }
}
