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

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.activiti.dmn.api.DecisionTable;
import org.activiti.dmn.api.DecisionTableQuery;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.form.api.FormDefinition;
import org.activiti.form.api.FormDefinitionQuery;
import org.activiti.form.api.FormRepositoryService;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yvo Swillens
 */
public class GetFormDefinitionsForProcessDefinitionCmd implements Command<List<FormDefinition>>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String processDefinitionId;
  protected FormRepositoryService formRepositoryService;

  public GetFormDefinitionsForProcessDefinitionCmd(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public List<FormDefinition> execute(CommandContext commandContext) {
    ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

    if (processDefinition == null) {
      throw new ActivitiObjectNotFoundException("Cannot find process definition for id: " + processDefinitionId, ProcessDefinition.class);
    }

    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

    if (bpmnModel == null) {
      throw new ActivitiObjectNotFoundException("Cannot find bpmn model for process definition id: " + processDefinitionId, BpmnModel.class);
    }

    if (commandContext.getProcessEngineConfiguration().isFormEngineInitialized() == false) {
      throw new ActivitiException("Form Engine is not initialized");
    } else {
      if (commandContext.getProcessEngineConfiguration().getFormEngineRepositoryService() == null) {
        throw new ActivitiException("Form repository service is not available");
      }
    }

    formRepositoryService = commandContext.getProcessEngineConfiguration().getFormEngineRepositoryService();
    List<FormDefinition> formDefinitions = getFormDefinitionsFromModel(bpmnModel, processDefinition);

    return formDefinitions;
  }

  protected List<FormDefinition> getFormDefinitionsFromModel(BpmnModel bpmnModel, ProcessDefinition processDefinition) {
    Set<String> formKeys = new HashSet<>();
    List<FormDefinition> formDefinitions = new ArrayList<>();

    // for all start events
    List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class, true);

    for (StartEvent startEvent : startEvents) {
      if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
        formKeys.add(startEvent.getFormKey());
      }
    }

    // for all user tasks
    List<UserTask> userTasks = bpmnModel.getMainProcess().findFlowElementsOfType(UserTask.class, true);

    for (UserTask userTask : userTasks) {
      if (StringUtils.isNotEmpty(userTask.getFormKey())) {
        formKeys.add(userTask.getFormKey());
      }
    }

    for (String formKey : formKeys) {
      addFormDefinitionToCollection(formDefinitions, formKey, processDefinition);
    }

    return formDefinitions;
  }

  protected void addFormDefinitionToCollection(List<FormDefinition> formDefinitions, String formKey, ProcessDefinition processDefinition) {
    FormDefinitionQuery formDefinitionQuery = formRepositoryService.createFormDefinitionQuery();
    FormDefinition formDefinition = formDefinitionQuery.formDefinitionKey(formKey).parentDeploymentId(processDefinition.getDeploymentId()).singleResult();

    if (formDefinition != null) {
      formDefinitions.add(formDefinition);
    }
  }
}
