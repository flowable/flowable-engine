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
import org.flowable.common.engine.api.variable.VariableContainer;

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

    public AbstractScriptEvaluator() {
    }

    public AbstractScriptEvaluator(Expression language, Expression script) {
        this.script = script;
        this.language = language;
    }

    /**
     * Validates language and script and creates a pre-populated {@link ScriptEngineRequest.Builder} which
     * can be evaluated using {@link #evaluateScriptRequest(ScriptEngineRequest.Builder)}.
     *
     * @return the ScriptEngineRequest builder instance for further population
     */
    public ScriptEngineRequest.Builder createScriptRequest(VariableContainer variableContainer) {
        validateParameters();

        String language = Objects.toString(this.language.getValue(variableContainer), null);
        if (language == null) {
            throw new FlowableIllegalStateException("'language' evaluated to null for listener of type 'script'");
        }
        String script = Objects.toString(this.script.getValue(variableContainer), null);
        if (script == null) {
            throw new FlowableIllegalStateException("Script content is null or evaluated to null for listener of type 'script'");
        }
        ScriptEngineRequest.Builder builder = ScriptEngineRequest.builder();

        return builder.setLanguage(language).setScript(script).setVariableContainer(variableContainer);
    }

    protected Object evaluateScriptRequest(ScriptEngineRequest.Builder requestBuilder) {
        ScriptEngineRequest request = requestBuilder.build();
        Object result = evaluateScript(getScriptingEngines(), request);

        if (resultVariable != null) {
            VariableContainer variableContainer = request.getVariableContainer();
            String resultVariable = Objects.toString(this.resultVariable.getValue(variableContainer), null);
            if (variableContainer != null && resultVariable != null) {
                variableContainer.setVariable(resultVariable, result);
            }
        }
        return result;
    }

    protected Object evaluateScript(ScriptingEngines scriptingEngines, ScriptEngineRequest request) {
        return scriptingEngines.evaluateWithEvaluationResult(request).getResult();
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
