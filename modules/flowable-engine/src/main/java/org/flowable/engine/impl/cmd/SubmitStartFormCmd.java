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

package org.flowable.engine.impl.cmd;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.form.StartFormHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SubmitStartFormCmd extends NeedsActiveProcessDefinitionCmd<ProcessInstance> {

    private static final long serialVersionUID = 1L;

    protected final String businessKey;
    protected Map<String, String> properties;

    public SubmitStartFormCmd(String processDefinitionId, String businessKey, Map<String, String> properties) {
        super(processDefinitionId);
        this.businessKey = businessKey;
        this.properties = properties;
    }

    @Override
    protected ProcessInstance execute(CommandContext commandContext, ProcessDefinitionEntity processDefinition) {
        if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext)) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            return compatibilityHandler.submitStartFormData(processDefinition.getId(), businessKey, properties);
        }

        ExecutionEntity processInstance = null;
        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();

        // TODO: backwards compatibility? Only create the process instance and not start it? How?
        if (businessKey != null) {
            processInstance = (ExecutionEntity) processInstanceHelper.createProcessInstance(processDefinition, businessKey, null, null, null);
        } else {
            processInstance = (ExecutionEntity) processInstanceHelper.createProcessInstance(processDefinition, null, null, null, null);
        }

        CommandContextUtil.getHistoryManager(commandContext).recordFormPropertiesSubmitted(processInstance.getExecutions().get(0), properties, null);

        FormHandlerHelper formHandlerHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFormHandlerHelper();
        StartFormHandler startFormHandler = formHandlerHelper.getStartFormHandler(commandContext, processDefinition);
        startFormHandler.submitFormProperties(properties, processInstance);

        processInstanceHelper.startProcessInstance(processInstance, commandContext, convertPropertiesToVariablesMap());

        return processInstance;
    }

    protected Map<String, Object> convertPropertiesToVariablesMap() {
        Map<String, Object> vars = new HashMap<>(properties.size());
        for (String key : properties.keySet()) {
            vars.put(key, properties.get(key));
        }
        return vars;
    }

}
