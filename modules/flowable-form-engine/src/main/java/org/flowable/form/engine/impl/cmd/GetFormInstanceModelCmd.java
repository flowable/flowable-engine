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
package org.flowable.form.engine.impl.cmd;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.form.api.FormInstance;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.FormExpression;
import org.flowable.form.engine.impl.interceptor.Command;
import org.flowable.form.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntity;
import org.flowable.form.model.ExpressionFormField;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormInstanceModel;
import org.flowable.form.model.FormModel;
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
    
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d-M-yyyy");

    protected String formInstanceId;
    protected String formDefinitionKey;
    protected String parentDeploymentId;
    protected String formDefinitionId;
    protected String taskId;
    protected String processInstanceId;
    protected String tenantId;
    protected Map<String, Object> variables;

    public GetFormInstanceModelCmd(String formInstanceId, Map<String, Object> variables) {
        initializeValues(null, null, null, null, null, null, variables);
        this.formInstanceId = formInstanceId;
    }

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
        if (formInstanceId == null && (taskId == null && processInstanceId == null)) {
            throw new FlowableException("A processtask id or process instance id should be provided");
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

            Map<String, JsonNode> formInstanceFieldMap = new HashMap<String, JsonNode>();
            if (formInstance != null) {
                fillFormInstanceValues(formInstanceModel, formInstance, formInstanceFieldMap, formEngineConfiguration.getObjectMapper());
                fillVariablesWithFormValues(formInstanceFieldMap, allFields);
            }

            for (FormField field : allFields) {
                if (field instanceof ExpressionFormField) {
                    ExpressionFormField expressionField = (ExpressionFormField) field;
                    FormExpression formExpression = formEngineConfiguration.getExpressionManager().createExpression(expressionField.getExpression());
                    try {
                        field.setValue(formExpression.getValue(variables));
                    } catch (Exception e) {
                        logger.error("Error getting value for expression {} {}", expressionField.getExpression(), e.getMessage());
                    }

                } else if (FormFieldTypes.UPLOAD.equals(field.getType())) {

                    // Multiple docs are stored as comma-separated string ids,
                    // explicitly storing them as an array so they're serialized properly
                    if (variables.containsKey(field.getId())) {
                        String uploadValue = (String) variables.get(field.getId());
                        if (uploadValue != null) {
                            List<String> contentIds = new ArrayList<>();
                            Collections.addAll(contentIds, uploadValue.split(","));
                            field.setValue(contentIds);
                        }
                    }

                } else {
                    Object variableValue = variables.get(field.getId());
                    if (variableValue != null) {
                        
                        if (variableValue instanceof LocalDate) {
                            field.setValue(((LocalDate) variableValue).toString("d-M-yyyy"));
                        } else if (variableValue instanceof Date) {
                            field.setValue((DATE_FORMAT.format((Date) variableValue)));
                        } else {
                            field.setValue(variableValue);
                        }
                    }
                }

                field.setReadOnly(true);
            }
        }
    }

    protected FormDefinitionCacheEntry resolveFormDefinition(CommandContext commandContext) {
        DeploymentManager deploymentManager = commandContext.getFormEngineConfiguration().getDeploymentManager();

        // Find the form definition
        FormDefinitionEntity formDefinitionEntity = null;

        if (formInstanceId != null) {

            FormInstanceEntity formInstanceEntity = commandContext.getFormEngineConfiguration().getFormInstanceDataManager().findById(formInstanceId);
            if (formInstanceEntity == null) {
                throw new FlowableObjectNotFoundException("No form instance found for id = '" + formInstanceId + "'", FormInstanceEntity.class);
            }

            formDefinitionEntity = deploymentManager.findDeployedFormDefinitionById(formInstanceEntity.getFormDefinitionId());
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'", FormDefinitionEntity.class);
            }

        } else if (formDefinitionId != null) {

            formDefinitionEntity = deploymentManager.findDeployedFormDefinitionById(formDefinitionId);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for id = '" + formDefinitionId + "'", FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId == null) {

            formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKey(formDefinitionKey);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "'", FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId == null) {

            formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndTenantId(formDefinitionKey, tenantId);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey + "' for tenant identifier " + tenantId, FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && (tenantId == null || FormEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

            formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyAndParentDeploymentId(formDefinitionKey, parentDeploymentId);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey +
                        "' for parent deployment id " + parentDeploymentId, FormDefinitionEntity.class);
            }

        } else if (formDefinitionKey != null && tenantId != null && !FormEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId != null) {

            formDefinitionEntity = deploymentManager.findDeployedLatestFormDefinitionByKeyParentDeploymentIdAndTenantId(formDefinitionKey, parentDeploymentId, tenantId);
            if (formDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No form definition found for key '" + formDefinitionKey +
                        "' for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, FormDefinitionEntity.class);
            }

        } else {
            throw new FlowableObjectNotFoundException("formDefinitionKey and formDefinitionId are null");
        }

        FormDefinitionCacheEntry formCacheEntry = deploymentManager.resolveFormDefinition(formDefinitionEntity);

        return formCacheEntry;
    }

    protected FormInstance resolveFormInstance(CommandContext commandContext) {
        FormEngineConfiguration formEngineConfiguration = commandContext.getFormEngineConfiguration();
        FormInstanceQuery formInstanceQuery = formEngineConfiguration.getFormService().createFormInstanceQuery().formDefinitionId(formDefinitionId);
        if (formInstanceId != null) {
            formInstanceQuery.id(formInstanceId);

        } else if (taskId != null) {
            formInstanceQuery.taskId(taskId);
            
        } else if (processInstanceId != null) {
            formInstanceQuery.processInstanceId(processInstanceId);
        
        } else {
            return null;
        }

        List<FormInstance> formInstances = formInstanceQuery.orderBySubmittedDate().asc().list();
        if (!formInstances.isEmpty()) {
            return formInstances.get(0);
        }

        return null;
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

            if (submittedNode.get("flowable_form_outcome") != null) {
                JsonNode outcomeNode = submittedNode.get("flowable_form_outcome");
                if (!outcomeNode.isNull() && StringUtils.isNotEmpty(outcomeNode.asText())) {
                    formInstanceModel.setSelectedOutcome(outcomeNode.asText());
                }
            }

        } catch (Exception e) {
            throw new FlowableException("Error parsing form instance " + formInstance.getId(), e);
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
                    logger.error("Error parsing form date value for process instance {} and task {} with value {}", processInstanceId, taskId, fieldValue, e);
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

        if (formInstance != null) {
            formInstanceModel.setFormInstanceId(formInstance.getId());
            formInstanceModel.setTaskId(formInstance.getTaskId());
            formInstanceModel.setProcessInstanceId(formInstance.getProcessInstanceId());
            formInstanceModel.setProcessDefinitionId(formInstance.getProcessDefinitionId());
            formInstanceModel.setSubmittedBy(formInstance.getSubmittedBy());
            formInstanceModel.setSubmittedDate(formInstance.getSubmittedDate());
        }

        return formInstanceModel;
    }
}
