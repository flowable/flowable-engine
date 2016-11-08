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
package org.activiti.app.service.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.app.domain.runtime.RelatedContent;
import org.activiti.app.model.component.SimpleContentTypeMapper;
import org.activiti.app.model.runtime.CompleteFormRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceVariableRepresentation;
import org.activiti.app.model.runtime.RelatedContentRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.service.exception.NotPermittedException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.model.FormField;
import org.activiti.form.model.FormFieldTypes;
import org.activiti.form.model.FormModel;
import org.activiti.idm.api.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class ActivitiTaskFormService {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiTaskFormService.class);

  @Autowired
  protected TaskService taskService;
  
  @Autowired
  protected RepositoryService repositoryService;
  
  @Autowired
  protected HistoryService historyService;

  @Autowired
  protected FormRepositoryService formRepositoryService;
  
  @Autowired
  protected FormService formService;

  @Autowired
  protected PermissionService permissionService;
  
  @Autowired
  protected RelatedContentService relatedContentService;
  
  @Autowired
  protected SimpleContentTypeMapper simpleTypeMapper;

  @Autowired
  protected ObjectMapper objectMapper;

  public FormModel getTaskForm(String taskId) {
    HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
    
    Map<String, Object> variables = new HashMap<String, Object>();
    if (task.getProcessInstanceId() != null) {
      List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
          .processInstanceId(task.getProcessInstanceId())
          .list();
      
      for (HistoricVariableInstance historicVariableInstance : variableInstances) {
        variables.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
      }
    }
    
    String parentDeploymentId = null;
    if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
      try {
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
        parentDeploymentId = processDefinition.getDeploymentId();
        
      } catch (ActivitiException e) {
        logger.error("Error getting process definition " + task.getProcessDefinitionId(), e);
      }
    }
    
    FormModel formModel = null;
    if (task.getEndTime() != null) {
      formModel = formService.getFormInstanceModelByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId, 
          taskId, task.getProcessInstanceId(), variables, task.getTenantId());
      
    } else {
      formModel = formService.getFormModelWithVariablesByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId, 
          task.getProcessInstanceId(), variables, task.getTenantId());
    }

    // If form does not exists, we don't want to leak out this info to just anyone
    if (formModel == null) {
      throw new NotFoundException("Form model for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
    }
    
    
    // TODO: needs to be moved to form service, but currently form service doesn't know about related content
    fetchRelatedContentInfoIfNeeded(formModel);

    return formModel;
  }
  
  protected void fetchRelatedContentInfoIfNeeded(FormModel formDefinition) {
    if (formDefinition.getFields() != null) {
      for (FormField formField : formDefinition.getFields()) {
        if (FormFieldTypes.UPLOAD.equals(formField.getType())) {
          
          List<String> relatedContentIds = null;
          if (formField.getValue() instanceof List) {
            relatedContentIds = (List<String>) formField.getValue();
          } else if (formField.getValue() instanceof String) {
            String[] splittedString = ((String) formField.getValue()).split(",");
            relatedContentIds = new ArrayList<String>();
            for (String relatedContentId : splittedString) {
              relatedContentIds.add(relatedContentId);
            }
          }
          
          List<RelatedContentRepresentation> relatedContentRepresentations = new ArrayList<RelatedContentRepresentation>();
          if (relatedContentIds != null) {
            for (String relatedContentId : relatedContentIds) {
              RelatedContent relatedContent = relatedContentService.get(relatedContentId);
              relatedContentRepresentations.add(new RelatedContentRepresentation(relatedContent, simpleTypeMapper));
            }
          }
          
          formField.setValue(relatedContentRepresentations);
        }
      }
    }
  }
  
  public void completeTaskForm(String taskId, CompleteFormRepresentation completeTaskFormRepresentation) {

    // Get the form definition
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

    if (task == null) {
      throw new NotFoundException("Task not found with id: " + taskId);
    }
    
    FormModel formModel = formRepositoryService.getFormModelById(completeTaskFormRepresentation.getFormId());

    User currentUser = SecurityUtils.getCurrentUserObject();
    if (!permissionService.isTaskOwnerOrAssignee(currentUser, taskId)) {
      if (!permissionService.validateIfUserIsInitiatorAndCanCompleteTask(currentUser, task)) {
        throw new NotPermittedException();
      }
    }
    

    // Extract raw variables and complete the task
    Map<String, Object> variables = formService.getVariablesFromFormSubmission(formModel, completeTaskFormRepresentation.getValues(),
        completeTaskFormRepresentation.getOutcome());
    
    formService.createFormInstance(variables, formModel, task.getId(), task.getProcessInstanceId());
    
    processUploadFieldsIfNeeded(currentUser, task, formModel, variables);
    
    taskService.complete(taskId, variables);
  }
  
  /**
   * When content is uploaded for a field, it is uploaded as a 'temporary related content'.
   * Now that the task is completed, we need to associate the field/taskId/processInstanceId 
   * with the related content so we can retrieve it later.
   */
  protected void processUploadFieldsIfNeeded(User currentUser, Task task, FormModel formDefinition, Map<String, Object> variables) {
    if (formDefinition != null && formDefinition.getFields() != null) {
      for (FormField formField : formDefinition.getFields()) {
        if (FormFieldTypes.UPLOAD.equals(formField.getType())) {
          
          String variableName = formField.getId();
          if (variables.containsKey(variableName)) {
            String variableValue = (String) variables.get(variableName);
            if (StringUtils.isNotEmpty(variableValue)) {
              String[] relatedContentIds = StringUtils.split(variableValue, ",");
              for (String relatedContentId : relatedContentIds) {
                
                // Only allowed to update content that was uploaded by user
                RelatedContent relatedContent = relatedContentService.get(relatedContentId);
                if (relatedContent.getCreatedBy() != null && relatedContent.getCreatedBy().equals(currentUser.getId())) {
                  relatedContentService.setContentField(relatedContentId, formField.getId(), task.getProcessInstanceId(), task.getId());
                } else {
                  throw new NotPermittedException();
                }
              }
            }
          }
          
        }
      }
    }
  }
  
  public List<ProcessInstanceVariableRepresentation> getProcessInstanceVariables(String taskId) {
    HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
    List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();

    // Get all process-variables to extract values from
    Map<String, ProcessInstanceVariableRepresentation> processInstanceVariables = new HashMap<String, ProcessInstanceVariableRepresentation>();

    for (HistoricVariableInstance historicVariableInstance : historicVariables) {
        ProcessInstanceVariableRepresentation processInstanceVariableRepresentation = new ProcessInstanceVariableRepresentation(
                historicVariableInstance.getVariableName(), historicVariableInstance.getVariableTypeName(), historicVariableInstance.getValue());
        processInstanceVariables.put(historicVariableInstance.getId(), processInstanceVariableRepresentation);
    }

    List<ProcessInstanceVariableRepresentation> processInstanceVariableRepresenations = 
        new ArrayList<ProcessInstanceVariableRepresentation>(processInstanceVariables.values());
    return processInstanceVariableRepresenations;
  }
}
