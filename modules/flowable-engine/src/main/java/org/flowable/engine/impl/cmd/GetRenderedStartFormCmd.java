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

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.impl.form.FormEngine;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.form.StartFormHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class GetRenderedStartFormCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;
    protected String formEngineName;

    public GetRenderedStartFormCmd(String processDefinitionId, String formEngineName) {
        this.processDefinitionId = processDefinitionId;
        this.formEngineName = formEngineName;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        ProcessDefinition processDefinition = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Process Definition '" + processDefinitionId + "' not found", ProcessDefinition.class);
        }

        if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, commandContext)) {
            return Flowable5Util.getFlowable5CompatibilityHandler().getRenderedStartForm(processDefinitionId, formEngineName);
        }

        FormHandlerHelper formHandlerHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFormHandlerHelper();
        StartFormHandler startFormHandler = formHandlerHelper.getStartFormHandler(commandContext, processDefinition);
        if (startFormHandler == null) {
            return null;
        }

        FormEngine formEngine = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFormEngines().get(formEngineName);

        if (formEngine == null) {
            throw new FlowableException("No formEngine '" + formEngineName + "' defined process engine configuration");
        }

        StartFormData startForm = startFormHandler.createStartFormData(processDefinition);

        return formEngine.renderStartForm(startForm);
    }
}
