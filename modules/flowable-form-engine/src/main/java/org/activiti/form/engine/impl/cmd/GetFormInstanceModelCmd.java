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
package org.activiti.form.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activiti.editor.form.converter.FormJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.form.api.FormInstance;
import org.activiti.form.api.FormInstanceQuery;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.FormExpression;
import org.activiti.form.engine.impl.interceptor.Command;
import org.activiti.form.engine.impl.interceptor.CommandContext;
import org.activiti.form.engine.impl.persistence.deploy.DeploymentManager;
import org.activiti.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.activiti.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.activiti.form.model.ExpressionFormField;
import org.activiti.form.model.FormField;
import org.activiti.form.model.FormFieldTypes;
import org.activiti.form.model.FormInstanceModel;
import org.activiti.form.model.FormModel;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class GetFormInstanceModelCmd implements Command<FormInstanceModel>, Serializable {

  private static Logger logger = LoggerFactory.getLogger(GetFormInstanceModelCmd.class);
  
  private static final long serialVersionUID = 1L;

  protected String formDefinitionKey;
  protected String parentDeploymentId;
  protected String formDefinitionId;
  protected String taskId;
  protected String processInstanceId;
  protected String tenantId;
  protected Map<String, Object> variables;
  
  public GetFormInstanceModelCmd(String formDefinitionKey, String formDefinitionId, String taskId, 
      String processInstanceId, Map<String, Object> variables) {
    
    initializeValues(formDefinitionKey, null, formDefinitionId, null, taskId, processInstanceId, variables);
  }
  
  public GetFormInstanceModelCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String taskId, 
      String processInstanceId, Map<String, Object> variables) {
    
    initializeValues(formDefinitionKey, parentDeploymentId, formDefinitionId, null, taskId, processInstanceId, variables);
  }
  
  public GetFormInstanceModelCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String taskId, 
      String processInstanceId, String tenantId, Map<String, Object> variables) {
    
    initializeValues(formDefinitionKey, parentDeploymentId, formDefinitionId, tenantId, taskId, processInstanceId, variables);
  }

  public FormInstanceModel execute(CommandContext commandContext) {
    if (taskId == null && processInstanceId == null) {
      throw new ActivitiException("A task id or process instance id should be provided");
    }
    
    FormDefinitionCacheEntry formDefinitionCacheEntry = resolveFormDefinition(commandContext);
    FormInstance formInstance = resolveFormInstance(commandContext);
    FormInstanceModel formInstanceModel = resolveFormInstanceModel(formDefinitionCacheEntry, formInstance, commandContext);
    fillFormFieldValues(formInstance, formInstanceModel, commandContext);
    return formInstanceModel;
  }
  
  protected void initializeValues(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String tenantId, 
      String taskId, String processInstanceId, Map<String, Object> variables) {
    
    this.formDefinitionKey = formDefinitionKey;
    this.parentDeploymentId = parentDeploymentId;
    this.formDefinitionId = formDefinitionId;
    this.tenantId = tenantId;
    this.taskId = taskId;
    this.processInstanceId = processInstanceId;
    if (variables != null) {
      this.variables = variables;
    } else {
      this.variables = new HashMap<String, Object>();
    }
  }

  protected void fillFormFieldValues(FormInstance formInstance, FormInstanceModel formInstanceModel, CommandContext commandContext) {

    FormEngineConfiguration formEngineConfiguration = commandContext.getFormEngineConfiguration();
    List<FormField> allFields = formInstanceModel.listAllFields();
    if (allFields != null) {

      Map<String, JsonNode> formInstanceFieldMap = fillPreviousFormValues(formInstance, formEngineConfiguration);
      fillFormInstanceValues(formInstanceModel, formInstance, formInstanceFieldMap, formEngineConfiguration.getObjectMapper());
      fillVariablesWithFormValues(formInstanceFieldMap, allFields);
      
      for (FormField field : allFields) {
        if (field instanceof ExpressionFormField) {
          ExpressionFormField expressionField = (ExpressionFormField) field;
          FormExpression formExpression = formEngineConfiguration.getExpressionManager().createExpression(expressionField.getExpression());
          try {
            field.setValue(formExpression.getValue(variables));
          } catch (Exception e) {
            logger.error("Error getting value for expression " + expressionField.getExpression() + " " + e.getMessage());
          }
          
        } else if (FormFieldTypes.UPLOAD.equals(field.getType())) {
          
          // Multiple docs are stored as comma-separated string ids,
          // explicitely storing them as an array so they're serialized properly
          if (variables.containsKey(field.getId())) {
            String uploadValue = (String) variables.get(field.getId());
            if (uploadValue != null) {
              List<String> contentIds = new ArrayList<>();
              for (String s : uploadValue.split(",")) {
                contentIds.add(s);
              }
              field.setValue(contentIds);
            }
          }
            
        } else {
          field.setValue(variables.get(field.getId()));
        }
        
        field.setReadOnly(true);
      }
    }
  }
  
  protected FormDefinitionCacheEntry resolveFormDefinition(CommandContext commandContext) {
    DeploymentManager deploymentManager = commandContext.getFormEngineConfiguration().getDeploymentManager();

    // Find the form definition
    FormDefinitionEntity formDefinitionEntity = null;
    if (formDefinitionId != null) {

      formDefinitionEntity = deploymentManager.findDeployedFormDefinitionById(formDefinitionId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'", FormDefinitionEntity.class);
      }

    } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId == null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKey(formDefinitionKey);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "'", FormDefinitionEntity.class);
      }

    } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)  && parentDeploymentId == null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "' for tenant identifier " + tenantId, FormDefinitionEntity.class);
      }
      
    } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndParentDeploymentId(formDefinitionKey, parentDeploymentId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + 
            "' for parent deployment id " + parentDeploymentId, FormDefinitionEntity.class);
      }
      
    } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)  && parentDeploymentId != null) {

      formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(formDefinitionKey, parentDeploymentId, tenantId);
      if (formDefinitionEntity == null) {
        throw new ActivitiObjectNotFoundException("No form definition found for key '" + formDefinitionKey + 
            "' for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, FormDefinitionEntity.class);
      }

    } else {
      throw new ActivitiObjectNotFoundException("formDefinitionKey and formDefinitionId are null");
    }

    FormDefinitionCacheEntry formCacheEntry = deploymentManager.resolveFormDefinition(formDefinitionEntity);
    
    return formCacheEntry;
  }
  
  protected FormInstance resolveFormInstance(CommandContext commandContext) {
    FormEngineConfiguration formEngineConfiguration = commandContext.getFormEngineConfiguration();
    FormInstanceQuery formInstanceQuery = formEngineConfiguration.getFormService().createFormInstanceQuery().formDefinitionId(formDefinitionId);
    if (taskId != null) {
      formInstanceQuery.taskId(taskId);
    } else {
      formInstanceQuery.processInstanceId(processInstanceId);
    }
    
    List<FormInstance> formInstances = formInstanceQuery.list();
    
    if (formInstances.size() == 0) {
      throw new ActivitiException("No form instance could be found");
    }
    
    FormInstance formInstance = null;
    if (taskId != null) {
      if (formInstances.size() > 1) {
        throw new ActivitiException("Multiple form instances are found for the same task");
      }
      
      formInstance = formInstances.get(0);
    
    } else {
      for (FormInstance formInstanceEntity : formInstances) {
        if (formInstanceEntity.getTaskId() == null) {
          formInstance = formInstanceEntity;
          break;
        }
      }
    }
    
    if (formInstance == null) {
      throw new ActivitiException("No form instance could be found");
    }
    
    return formInstance;
  }
  
  protected Map<String, JsonNode> fillPreviousFormValues(FormInstance formInstance, FormEngineConfiguration formEngineConfiguration) {
    Map<String, JsonNode> formInstancesMap = new HashMap<String, JsonNode>();
    if (taskId != null && processInstanceId != null) {
      List<FormInstance> formInstances = formEngineConfiguration.getFormService().createFormInstanceQuery()
        .processInstanceId(processInstanceId)
        .submittedDateBefore(formInstance.getSubmittedDate())
        .orderBySubmittedDate()
        .desc()
        .list();

      for (FormInstance otherFormInstance : formInstances) {
        if (otherFormInstance.getId().equals(formInstance.getId())) {
          continue;
        }
        
        try {
          JsonNode submittedNode = formEngineConfiguration.getObjectMapper().readTree(formInstance.getFormValueBytes());
          if (submittedNode == null || submittedNode.get("values") != null) {
            continue;
          }
         
          JsonNode valuesNode = submittedNode.get("values");
          Iterator<String> fieldIdIterator = valuesNode.fieldNames();
          while (fieldIdIterator.hasNext()) {
            String fieldId = fieldIdIterator.next();
            if (formInstancesMap.containsKey(fieldId) == false) {
  
              JsonNode valueNode = valuesNode.get(fieldId);
              formInstancesMap.put(fieldId, valueNode);
            }
          }

        } catch (Exception e) {
          throw new ActivitiException("Error parsing form instance " + formInstance.getId());
        }
      }
    }
    
    return formInstancesMap;
  }
  
  protected void fillFormInstanceValues(FormInstanceModel formInstanceModel, FormInstance formInstance, 
      Map<String, JsonNode> formInstanceFieldMap, ObjectMapper objectMapper) {
    
    try {
      JsonNode submittedNode = objectMapper.readTree(formInstance.getFormValueBytes());
      if (submittedNode == null) {
        return;
      }
     
      if (submittedNode.get("values") != null) {
        JsonNode valuesNode = submittedNode.get("values");
        Iterator<String> fieldIdIterator = valuesNode.fieldNames();
        while (fieldIdIterator.hasNext()) {
          String fieldId = fieldIdIterator.next();
          JsonNode valueNode = valuesNode.get(fieldId);
          formInstanceFieldMap.put(fieldId, valueNode);
        }
      }
      
      if (submittedNode.get("outcome") != null) {
        JsonNode outcomeNode = submittedNode.get("outcome");
        if (outcomeNode.isNull() == false && StringUtils.isNotEmpty(outcomeNode.asText())) {
          formInstanceModel.setSelectedOutcome(outcomeNode.asText());
        }
      }

    } catch (Exception e) {
      throw new ActivitiException("Error parsing form instance " + formInstance.getId(), e);
    }
  }
  
  public void fillVariablesWithFormValues(Map<String, JsonNode> submittedFormFieldMap, List<FormField> allFields) {
    for (FormField field : allFields) {
      
      JsonNode fieldValueNode = submittedFormFieldMap.get(field.getId());
  
      if (fieldValueNode == null || fieldValueNode.isNull()) {
        continue;
      }
  
      String fieldType = field.getType();
      String fieldValue = fieldValueNode.asText();
  
      if (FormFieldTypes.DATE.equals(fieldType)) {
        try {
          if (StringUtils.isNotEmpty(fieldValue)) {
            LocalDate dateValue = LocalDate.parse(fieldValue);
            variables.put(field.getId(), dateValue.toString("d-M-yyyy"));
          }
        } catch (Exception e) {
          logger.error("Error parsing form date value for process instance " + processInstanceId + " and task " + taskId + " with value " + fieldValue, e);
        }
  
      } else {
        variables.put(field.getId(), fieldValue);
      }
    }
  }
  
  protected FormInstanceModel resolveFormInstanceModel(FormDefinitionCacheEntry formCacheEntry, 
      FormInstance formInstance, CommandContext commandContext) {
    
    FormDefinitionEntity formDefinitionEntity = formCacheEntry.getFormDefinitionEntity();
    FormJsonConverter formJsonConverter = commandContext.getFormEngineConfiguration().getFormJsonConverter();
    FormModel formModel = formJsonConverter.convertToFormModel(formCacheEntry.getFormDefinitionJson(), 
        formDefinitionEntity.getId(), formDefinitionEntity.getVersion());
    FormInstanceModel formInstanceModel = new FormInstanceModel(formModel);
    formInstanceModel.setId(formDefinitionEntity.getId());
    formInstanceModel.setName(formDefinitionEntity.getName());
    formInstanceModel.setKey(formDefinitionEntity.getKey());
    formInstanceModel.setTenantId(formDefinitionEntity.getTenantId());
    
    formInstanceModel.setFormInstanceId(formInstance.getId());
    formInstanceModel.setTaskId(formInstance.getTaskId());
    formInstanceModel.setProcessInstanceId(formInstance.getProcessInstanceId());
    formInstanceModel.setProcessDefinitionId(formInstance.getProcessDefinitionId());
    formInstanceModel.setSubmittedBy(formInstance.getSubmittedBy());
    formInstanceModel.setSubmittedDate(formInstance.getSubmittedDate());
    
    return formInstanceModel;
  }
}