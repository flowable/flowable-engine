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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.editor.form.converter.FormJsonConverter;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.form.engine.impl.persistence.deploy.FormDefinitionCacheEntry;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.model.ExpressionFormField;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.Option;
import org.flowable.form.model.OptionFormField;
import org.flowable.form.model.SimpleFormModel;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class GetFormModelWithVariablesCmd implements Command<FormInfo>, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetFormModelWithVariablesCmd.class);

    private static final long serialVersionUID = 1L;
    
    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-M-d");

    protected String formDefinitionKey;
    protected String parentDeploymentId;
    protected String formDefinitionId;
    protected String taskId;
    protected String tenantId;
    protected Map<String, Object> variables;

    public GetFormModelWithVariablesCmd(String formDefinitionKey, String formDefinitionId, String taskId, Map<String, Object> variables) {
        initializeValues(formDefinitionKey, formDefinitionId, null, variables);
        this.taskId = taskId;
    }

    public GetFormModelWithVariablesCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String taskId, Map<String, Object> variables) {
        initializeValues(formDefinitionKey, formDefinitionId, null, variables);
        this.parentDeploymentId = parentDeploymentId;
        this.taskId = taskId;
    }

    public GetFormModelWithVariablesCmd(String formDefinitionKey, String parentDeploymentId, String formDefinitionId, String taskId, String tenantId, Map<String, Object> variables) {
        initializeValues(formDefinitionKey, formDefinitionId, null, variables);
        this.parentDeploymentId = parentDeploymentId;
        this.taskId = taskId;
        this.tenantId = tenantId;
    }

    @Override
    public FormInfo execute(CommandContext commandContext) {
        FormDefinitionCacheEntry formCacheEntry = resolveFormDefinition(commandContext);
        FormInstance formInstance = resolveFormInstance(formCacheEntry, commandContext);
        FormInfo formInfo = resolveFormModel(formCacheEntry, commandContext);
        fillFormFieldValues(formInstance, formInfo, commandContext);
        return formInfo;
    }

    protected void initializeValues(String formDefinitionKey, String formDefinitionId, String tenantId, Map<String, Object> variables) {
        this.formDefinitionKey = formDefinitionKey;
        this.formDefinitionId = formDefinitionId;
        this.tenantId = tenantId;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new HashMap<>();
        }
    }

    protected void fillFormFieldValues(FormInstance formInstance, FormInfo formInfo, CommandContext commandContext) {

        FormEngineConfiguration formEngineConfiguration = CommandContextUtil.getFormEngineConfiguration();
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        List<FormField> allFields = formModel.listAllFields();
        if (allFields != null) {

            Map<String, JsonNode> formInstanceFieldMap = new HashMap<>();
            if (formInstance != null) {
                fillFormInstanceValues(formInstance, formInstanceFieldMap, formEngineConfiguration.getObjectMapper());
                fillVariablesWithFormInstanceValues(formInstanceFieldMap, allFields, formInstance.getId());
            }

            for (FormField field : allFields) {
                if (field instanceof OptionFormField) {
                    // Drop down options to be populated from an expression
                    OptionFormField optionFormField = (OptionFormField) field;
                    if(optionFormField.getOptionsExpression() != null) {
                        Expression optionsExpression = formEngineConfiguration.getExpressionManager().createExpression(optionFormField.getOptionsExpression());
                        Object value = null;
                        try {
                            value = optionsExpression.getValue(new VariableContainerWrapper(variables));
                        } catch (Exception e) {
                            throw new FlowableException("Error getting value for optionsExpression: " + optionFormField.getOptionsExpression(), e);
                        }
                        if(value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Option> options = (List<Option>) value;
                            optionFormField.setOptions(options);
                        } else if(value instanceof String) {
                            String json = (String) value;
                            try {
                                List<Option> options = formEngineConfiguration.getObjectMapper().readValue(json, new TypeReference<List<Option>>(){});
                                optionFormField.setOptions(options);
                            } catch (Exception e) {
                                throw new FlowableException("Error parsing optionsExpression json value: " + json, e);
                            }
                        } else {
                            throw new FlowableException("Invalid type from evaluated expression for optionsExpression: " + optionFormField.getOptionsExpression() + ", resulting type:" + value.getClass().getName());
                        }
                    }
                    Object variableValue = variables.get(field.getId());
                    optionFormField.setValue(variableValue);
                } else if (FormFieldTypes.HYPERLINK.equals(field.getType())) {
                    Object variableValue = variables.get(field.getId());
                    // process expression if there is no value, otherwise keep it
                    if (variableValue != null) {
                        field.setValue(variableValue);
                    } else {
                        // No value set, process as expression
                        if (field.getParam("hyperlinkUrl") != null) {
                            String hyperlinkUrl = field.getParam("hyperlinkUrl").toString();
                            Expression formExpression = formEngineConfiguration.getExpressionManager().createExpression(hyperlinkUrl);
                            try {
                                field.setValue(formExpression.getValue(new VariableContainerWrapper(variables)));
                            } catch (Exception e) {
                                LOGGER.error("Error getting value for hyperlink expression {} {}", hyperlinkUrl, e.getMessage(), e);
                            }
                        }
                    }
                } else if (field instanceof ExpressionFormField) {
                    ExpressionFormField expressionField = (ExpressionFormField) field;
                    Expression formExpression = formEngineConfiguration.getExpressionManager().createExpression(expressionField.getExpression());
                    try {
                        field.setValue(formExpression.getValue(new VariableContainerWrapper(variables)));
                    } catch (Exception e) {
                        LOGGER.error("Error getting value for expression {} {}", expressionField.getExpression(), e.getMessage(), e);
                    }

                } else {
                    Object variableValue = variables.get(field.getId());
                    
                    if (variableValue != null) {
                        if (variableValue instanceof LocalDate) {
                            LocalDate dateVariable = (LocalDate) variableValue;
                            field.setValue(dateVariable.toString("yyyy-M-d"));
                            
                        } else if (variableValue instanceof Date) {
                            Date dateVariable = (Date) variableValue;
                            field.setValue(DATE_FORMAT.format(dateVariable));
                            
                        } else {
                            field.setValue(variableValue);
                        }
                    }
                }
            }
        }
    }

    protected FormDefinitionCacheEntry resolveFormDefinition(CommandContext commandContext) {
        DeploymentManager deploymentManager = CommandContextUtil.getFormEngineConfiguration().getDeploymentManager();

        // Find the form definition
        FormDefinitionEntity formDefinitionEntity = null;
        if (formDefinitionId != null) {

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

    protected void fillFormInstanceValues(
            FormInstance formInstance, Map<String, JsonNode> formInstanceFieldMap, ObjectMapper objectMapper) {

        if (formInstance == null) {
            return;
        }

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

        } catch (Exception e) {
            throw new FlowableException("Error parsing form instance " + formInstance.getId(), e);
        }
    }

    public void fillVariablesWithFormInstanceValues(Map<String, JsonNode> formInstanceFieldMap, List<FormField> allFields, String formInstanceId) {
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
                        variables.put(field.getId(), dateValue.toString("yyyy-M-d"));
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Error parsing form date value for form instance {} with value {}", formInstanceId, fieldValue, e);
                }
                
            } else if (fieldValueNode.isBoolean()) {
                variables.put(field.getId(), fieldValueNode.asBoolean());
                
            } else if (fieldValueNode.isLong()) {
                variables.put(field.getId(), fieldValueNode.asLong());
                
            } else if (fieldValueNode.isDouble()) {
                variables.put(field.getId(), fieldValueNode.asDouble());

            } else {
                variables.put(field.getId(), fieldValue);
            }
        }
    }

    protected FormInstance resolveFormInstance(FormDefinitionCacheEntry formCacheEntry, CommandContext commandContext) {
        if (taskId == null) {
            return null;
        }
        
        List<FormInstance> formInstances = CommandContextUtil.getFormEngineConfiguration().getFormService()
                .createFormInstanceQuery().formDefinitionId(formCacheEntry.getFormDefinitionEntity().getId())
                .taskId(taskId)
                .orderBySubmittedDate()
                .desc()
                .list();

        if (formInstances.size() > 0) {
            return formInstances.get(0);
        }

        return null;
    }

    protected FormInfo resolveFormModel(FormDefinitionCacheEntry formCacheEntry, CommandContext commandContext) {
        FormDefinitionEntity formEntity = formCacheEntry.getFormDefinitionEntity();
        FormJsonConverter formJsonConverter = CommandContextUtil.getFormEngineConfiguration().getFormJsonConverter();
        SimpleFormModel formModel = formJsonConverter.convertToFormModel(formCacheEntry.getFormDefinitionJson());
        FormInfo formInfo = new FormInfo();
        formInfo.setId(formEntity.getId());
        formInfo.setName(formEntity.getName());
        formInfo.setKey(formEntity.getKey());
        formInfo.setVersion(formEntity.getVersion());
        formInfo.setFormModel(formModel);

        return formInfo;
    }
}
