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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.activiti.editor.form.converter.FormJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.form.api.FormInstance;
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
import org.activiti.form.model.FormModel;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Tijs Rademakers
 */
public class GetFormModelWithVariablesCmd implements Command<FormModel>, Serializable {

  private static Logger logger = LoggerFactory.getLogger(GetFormModelWithVariablesCmd.class);
  
  private static final long serialVersionUID = 1L;

  protected String formDefinitionKey;
  protected String parentDeploymentId;
  protected String formDefinitionId;
  protected String processInstanceId;
  protected String tenantId;
  protected Map<String, Object> variables;
  
  public GetFormModelWithVariablesCmd(String formDefinitionKey, String formDefinitionId, String processInstanceId, Map<String, Object> variables) {
    initializeValues(formDefinitionKey, formDefinitionId, null, variables);
    this.processInstanceId = processInstanceId;
  }
  
  public GetFormModelWithVariablesCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String processInstanceId, Map<String, Object> variables) {
    initializeValues(formDefinitionKey, formDefinitionId, null, variables);
    this.parentDeploymentId = parentDeploymentId;
    this.processInstanceId = processInstanceId;
  }
  
  public GetFormModelWithVariablesCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String processInstanceId, String tenantId, Map<String, Object> variables) {
    initializeValues(formDefinitionKey, formDefinitionId, null, variables);
    this.parentDeploymentId = parentDeploymentId;
    this.processInstanceId = processInstanceId;
    this.tenantId = tenantId;
  }

  public FormModel execute(CommandContext commandContext) {
    FormDefinitionCacheEntry formCacheEntry = resolveFormDefinition(commandContext);
    FormModel formModel = resolveFormModel(formCacheEntry, commandContext);
    fillFormFieldValues(formModel, commandContext);
    return formModel;
  }
  
  protected void initializeValues(String formDefinitionKey, String formDefinitionId, String tenantId, Map<String, Object> variables) {
    this.formDefinitionKey = formDefinitionKey;
    this.formDefinitionId = formDefinitionId;
    this.tenantId = tenantId;
    if (variables != null) {
      this.variables = variables;
    } else {
      this.variables = new HashMap<String, Object>();
    }
  }

  protected void fillFormFieldValues(FormModel formDefinition, CommandContext commandContext) {

    FormEngineConfiguration formEngineConfiguration = commandContext.getFormEngineConfiguration();
    List<FormField> allFields = formDefinition.listAllFields();
    if (allFields != null) {

      Map<String, JsonNode> formInstanceFieldMap = fillPreviousFormInstanceValues(formEngineConfiguration);
      fillVariablesWithFormInstanceValues(formInstanceFieldMap, allFields);
      
      for (FormField field : allFields) {
        if (field instanceof ExpressionFormField) {
          ExpressionFormField expressionField = (ExpressionFormField) field;
          FormExpression formExpression = formEngineConfiguration.getExpressionManager().createExpression(expressionField.getExpression());
          try {
            field.setValue(formExpression.getValue(variables));
          } catch (Exception e) {
            logger.error("Error getting value for expression " + expressionField.getExpression() + " " + e.getMessage(), e);
          }
          
        } else {
          field.setValue(variables.get(field.getId()));
        }
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

    } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId == null) {

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
  
  protected Map<String, JsonNode> fillPreviousFormInstanceValues(FormEngineConfiguration formEngineConfiguration) {
    Map<String, JsonNode> formInstanceMap = new HashMap<String, JsonNode>();
    if (processInstanceId != null) {
      List<FormInstance> formInstances = formEngineConfiguration.getFormService().createFormInstanceQuery()
        .processInstanceId(processInstanceId)
        .orderBySubmittedDate()
        .desc()
        .list();

      for (FormInstance otherFormInstance : formInstances) {
        try {
          JsonNode submittedNode = formEngineConfiguration.getObjectMapper().readTree(otherFormInstance.getFormValueBytes());
          if (submittedNode == null || submittedNode.get("values") != null) {
            continue;
          }
         
          JsonNode valuesNode = submittedNode.get("values");
          Iterator<String> fieldIdIterator = valuesNode.fieldNames();
          while (fieldIdIterator.hasNext()) {
            String fieldId = fieldIdIterator.next();
            if (formInstanceMap.containsKey(fieldId) == false) {
  
              JsonNode valueNode = valuesNode.get(fieldId);
              formInstanceMap.put(fieldId, valueNode);
            }
          }

        } catch (Exception e) {
          throw new ActivitiException("Error parsing form instance " + otherFormInstance.getId());
        }
      }
    }
    
    return formInstanceMap;
  }
  
  public void fillVariablesWithFormInstanceValues(Map<String, JsonNode> formInstanceFieldMap, List<FormField> allFields) {
    for (FormField field : allFields) {
      
      JsonNode fieldValueNode = formInstanceFieldMap.get(field.getId());
  
      if (fieldValueNode == null || fieldValueNode.isNull()) {
        continue;
      }
  
      String fieldType = field.getType();
      String fieldValue = fieldValueNode.asText();
  
      if (FormFieldTypes.DATE.equals(fieldType)) {
        try {
          if (StringUtils.isNotEmpty(fieldValue)) {
            LocalDate dateValue = LocalDate.parse(fieldValue);
            variables.put(field.getId(), dateValue);
          }
        } catch (Exception e) {
          logger.error("Error parsing form date value for process instance " + processInstanceId + " with value " + fieldValue, e);
        }
  
      } else {
        variables.put(field.getId(), fieldValue);
      }
    }
  }
  
  protected FormModel resolveFormModel(FormDefinitionCacheEntry formCacheEntry, CommandContext commandContext) {
    FormDefinitionEntity formEntity = formCacheEntry.getFormDefinitionEntity();
    FormJsonConverter formJsonConverter = commandContext.getFormEngineConfiguration().getFormJsonConverter();
    FormModel formDefinition = formJsonConverter.convertToFormModel(formCacheEntry.getFormDefinitionJson(), formEntity.getId(), formEntity.getVersion());
    formDefinition.setId(formEntity.getId());
    formDefinition.setName(formEntity.getName());
    formDefinition.setKey(formEntity.getKey());
    
    return formDefinition;
  }
}