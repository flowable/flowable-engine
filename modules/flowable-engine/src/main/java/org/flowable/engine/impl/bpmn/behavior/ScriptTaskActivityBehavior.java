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
package org.flowable.engine.impl.bpmn.behavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implementation of the BPMN 2.0 script task.
 * 
 * @author Joram Barrez
 * @author Christian Stettler
 * @author Falko Menge
 */
public class ScriptTaskActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTaskActivityBehavior.class);

    protected String scriptTaskId;
    protected String script;
    protected String language;
    protected String resultVariable;
    protected boolean storeScriptVariables; // see https://activiti.atlassian.net/browse/ACT-1626

    public ScriptTaskActivityBehavior(String script, String language, String resultVariable) {
        this.script = script;
        this.language = language;
        this.resultVariable = resultVariable;
    }

    public ScriptTaskActivityBehavior(String scriptTaskId, String script, String language, String resultVariable, boolean storeScriptVariables) {
        this(script, language, resultVariable);
        this.scriptTaskId = scriptTaskId;
        this.storeScriptVariables = storeScriptVariables;
    }

    @Override
    public void execute(DelegateExecution execution) {

        ScriptingEngines scriptingEngines = CommandContextUtil.getProcessEngineConfiguration().getScriptingEngines();

        if (CommandContextUtil.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(scriptTaskId, execution.getProcessDefinitionId());
            if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SCRIPT_TASK_SCRIPT)) {
                String overrideScript = taskElementProperties.get(DynamicBpmnConstants.SCRIPT_TASK_SCRIPT).asText();
                if (StringUtils.isNotEmpty(overrideScript) && !overrideScript.equals(script)) {
                    script = overrideScript;
                }
            }
        }

        boolean noErrors = true;
        try {
            Object result = scriptingEngines.evaluate(script, language, execution, storeScriptVariables);

            if (resultVariable != null) {
                execution.setVariable(resultVariable, result);
            }

        } catch (FlowableException e) {

            LOGGER.warn("Exception while executing {} : {}", execution.getCurrentFlowElement().getId(), e.getMessage());

            noErrors = false;
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof BpmnError) {
                ErrorPropagation.propagateError((BpmnError) rootCause, execution);
            } else {
                throw e;
            }
        }
        if (noErrors) {
            leave(execution);
        }
    }

}
