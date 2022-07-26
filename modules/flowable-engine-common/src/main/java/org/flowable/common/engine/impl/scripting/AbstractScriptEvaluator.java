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

import java.util.Objects;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Base class simplifying binding and evaluation of scriptable elements.
 *
 * @author Rich Kroll
 * @author Joram Barrez
 * @author Arthur Hupka-Merle
 */
public abstract class AbstractScriptEvaluator {

    private static final long serialVersionUID = -8915149072831499057L;

    /**
     * The language of the script e.g. an Expression evaluating to javascript, juel, groovy, etc. Mandatory.
     * <p/>
     * Must not be or evaluate to <code>null</code> to null.
     */
    protected Expression language;

    /**
     * The actual payload of the script in the given language. Mandatory.
     * <p/>
     * Must not be or evaluate to <code>null</code> to null.
     */
    protected Expression script;

    /**
     * The name of the result variable to store the result of the script evaluation in the
     * variableScope.
     */
    protected Expression resultVariable;

    /**
     * Hint for the ScriptingEngine to automatically add
     * variables which are locally defined in the script, to
     * as execution variable.
     */
    protected boolean autoStoreVariables;

    public AbstractScriptEvaluator() {
    }

    public AbstractScriptEvaluator(Expression language, Expression script) {
        this.script = script;
        this.language = language;
    }

    /**
     * Validates that required parameters are present and evaluates the script.
     * <p/>
     * Handles setting the result value of the script to the given variable scope, in case {@link #resultVariable} is set.
     *
     * @param variableScope the variable scope used for the script execution. Available in the script context.
     * @return the result of the script evaluation.
     */
    public Object validateParametersAndEvaluteScript(VariableScope variableScope) {
        validateParameters();

        String language = Objects.toString(this.language.getValue(variableScope), null);
        if (language == null) {
            throw new FlowableIllegalStateException("'language' evaluated to null for listener of type 'script'");
        }
        String script = Objects.toString(this.script.getValue(variableScope), null);
        if (script == null) {
            throw new FlowableIllegalStateException("Script content is null or evaluated to null for listener of type 'script'");
        }

        Object result = evaluateScript(variableScope, getScriptingEngines(), language, script);

        if (resultVariable != null) {
            String resultVariable = Objects.toString(this.resultVariable.getValue(variableScope), null);
            if (resultVariable != null) {
                variableScope.setVariable(resultVariable, result);
            }
        }
        return result;
    }

    protected Object evaluateScript(VariableScope variableScope, ScriptingEngines scriptingEngines, String language, String script) {
        return scriptingEngines.evaluate(script, language, variableScope, autoStoreVariables);
    }

    protected void validateParameters() {
        if (script == null) {
            throw new IllegalArgumentException("The field 'script' should be set on the TaskListener");
        }

        if (language == null) {
            throw new IllegalArgumentException("The field 'language' should be set on the TaskListener");
        }
    }

    protected abstract ScriptingEngines getScriptingEngines();

    public void setScript(Expression script) {
        this.script = script;
    }

    public void setLanguage(Expression language) {
        this.language = language;
    }

    public void setResultVariable(Expression resultVariable) {
        this.resultVariable = resultVariable;
    }
}
