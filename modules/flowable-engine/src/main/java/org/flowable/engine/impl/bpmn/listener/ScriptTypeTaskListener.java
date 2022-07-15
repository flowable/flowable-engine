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
package org.flowable.engine.impl.bpmn.listener;

import java.util.Objects;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Implementation for the {@code <taskListener type="script" />} TaskListener extension.
 *
 * @author Rich Kroll
 * @author Joram Barrez
 * @author Arthur Hupka-Merle
 */
public class ScriptTypeTaskListener implements TaskListener {

    private static final long serialVersionUID = -8915149072830499057L;

    protected Expression script;

    protected Expression language;

    protected Expression resultVariable;

    public ScriptTypeTaskListener() {
    }

    public ScriptTypeTaskListener(Expression language, Expression script) {
        this.script = script;
        this.language = language;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        validateParameters();

        ScriptingEngines scriptingEngines = CommandContextUtil.getProcessEngineConfiguration().getScriptingEngines();
        String language = Objects.toString(this.language.getValue(delegateTask), null);
        if (language == null) {
            throw new FlowableIllegalStateException("'language' evaluated to null for taskListener of type 'script'");
        }
        String script = Objects.toString(this.script.getValue(delegateTask), null);
        if (script == null) {
            throw new FlowableIllegalStateException("Script content is null or evaluated to null for taskListener of type 'script'");
        }

        Object result = scriptingEngines.evaluate(script, language, delegateTask, false);

        if (resultVariable != null) {
            String resultVariable = Objects.toString(this.resultVariable.getValue(delegateTask), null);
            if (resultVariable != null) {
                delegateTask.setVariable(resultVariable, result);
            }
        }
    }

    protected void validateParameters() {
        if (script == null) {
            throw new IllegalArgumentException("The field 'script' should be set on the TaskListener");
        }

        if (language == null) {
            throw new IllegalArgumentException("The field 'language' should be set on the TaskListener");
        }
    }

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