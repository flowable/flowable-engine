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
package org.flowable.scripting.secure.behavior;

import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.scripting.secure.SecureJavascriptConfigurator;
import org.flowable.scripting.secure.impl.SecureJavascriptUtil;

/**
 * @author Joram Barrez
 */
public class SecureJavascriptTaskActivityBehavior extends ScriptTaskActivityBehavior {

    public SecureJavascriptTaskActivityBehavior(String scriptTaskId, String script,
        String language, String resultVariable, String skipExpression,
        boolean storeScriptVariables) {
        super(scriptTaskId, script, language, resultVariable, skipExpression, storeScriptVariables);
    }

    @Override
    protected void executeScript(DelegateExecution execution) {
        Map<Object, Object> beans = null;
        if (SecureJavascriptConfigurator.secureScriptContextFactory.isEnableAccessToBeans()) {
            beans = CommandContextUtil.getProcessEngineConfiguration().getBeans();
        }
        Object result = SecureJavascriptUtil.evaluateScript(execution, script, beans);

        if (resultVariable != null) {
            execution.setVariable(resultVariable, result);
        }
    }

}
