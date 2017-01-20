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

package org.activiti.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.common.api.ActivitiObjectNotFoundException;
import org.activiti.engine.compatibility.Activiti5CompatibilityHandler;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.form.StartFormHandler;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.util.Activiti5Util;
import org.activiti.engine.impl.util.FormHandlerUtil;
import org.activiti.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 */
public class GetStartFormCmd implements Command<StartFormData>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String processDefinitionId;

  public GetStartFormCmd(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public StartFormData execute(CommandContext commandContext) {
    ProcessDefinition processDefinition = commandContext.getProcessEngineConfiguration().getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
    if (processDefinition == null) {
      throw new ActivitiObjectNotFoundException("No process definition found for id '" + processDefinitionId + "'", ProcessDefinition.class);
    }
    
    if (commandContext.getProcessEngineConfiguration().isActiviti5CompatibilityEnabled() && 
        Activiti5CompatibilityHandler.ACTIVITI_5_ENGINE_TAG.equals(processDefinition.getEngineVersion())) {
      
      return Activiti5Util.getActiviti5CompatibilityHandler().getStartFormData(processDefinitionId);
    }

    StartFormHandler startFormHandler = FormHandlerUtil.getStartFormHandler(commandContext, processDefinition);
    if (startFormHandler == null) {
      throw new ActivitiException("No startFormHandler defined in process '" + processDefinitionId + "'");
    }

    return startFormHandler.createStartFormData(processDefinition);
  }
  
}
