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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.content.api.ContentItem;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.form.model.FormField;
import org.activiti.form.model.FormFieldTypes;
import org.activiti.form.model.FormModel;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 */
public class GetStartFormModelCmd implements Command<FormModel>, Serializable {

  private static final long serialVersionUID = 1L;
  
  protected String processDefinitionId;
  protected String processInstanceId;

  public GetStartFormModelCmd(String processDefinitionId, String processInstanceId) {
    this.processDefinitionId = processDefinitionId;
    this.processInstanceId = processInstanceId;
  }

  public FormModel execute(CommandContext commandContext) {
    ProcessEngineConfigurationImpl processEngineConfiguration = commandContext.getProcessEngineConfiguration();
    if (processEngineConfiguration.isFormEngineInitialized() == false) {
      throw new ActivitiIllegalArgumentException("Form engine is not initialized");
    }
    
    FormModel formModel = null;
    ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);
    Process process = bpmnModel.getProcessById(processDefinition.getKey());
    FlowElement startElement = process.getInitialFlowElement();
    if (startElement instanceof StartEvent) {
      StartEvent startEvent = (StartEvent) startElement;
      if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
        formModel = processEngineConfiguration.getFormEngineFormService().getFormInstanceModelByKeyAndParentDeploymentId(
            startEvent.getFormKey(), processDefinition.getDeploymentId(), null, processInstanceId, null, processDefinition.getTenantId());
      }
    }

    // If form does not exists, we don't want to leak out this info to just anyone
    if (formModel == null) {
      throw new ActivitiObjectNotFoundException("Form model for process definition " + processDefinitionId + " cannot be found");
    }
    
    fetchRelatedContentInfoIfNeeded(formModel, processEngineConfiguration);

    return formModel;
  }
  
  protected void fetchRelatedContentInfoIfNeeded(FormModel formModel, ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (processEngineConfiguration.isContentEngineInitialized() == false) {
      return;
    }
    
    if (formModel.getFields() != null) {
      for (FormField formField : formModel.getFields()) {
        if (FormFieldTypes.UPLOAD.equals(formField.getType())) {
          
          List<String> contentItemIds = null;
          if (formField.getValue() instanceof List) {
            contentItemIds = (List<String>) formField.getValue();
            
          } else if (formField.getValue() instanceof String) {
            String[] splittedString = ((String) formField.getValue()).split(",");
            contentItemIds = new ArrayList<String>();
            for (String contentItemId : splittedString) {
              contentItemIds.add(contentItemId);
            }
          }
          
          if (contentItemIds != null) {
            Set<String> contentItemIdSet = new HashSet<>(contentItemIds);
            
            List<ContentItem> contentItems = processEngineConfiguration.getContentService()
                .createContentItemQuery()
                .ids(contentItemIdSet)
                .list();
            
            formField.setValue(contentItems);
          }
        }
      }
    }
  }

}
