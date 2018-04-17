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
import java.util.Date;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntity;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.SimpleFormModel;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractSaveFormInstanceCmd implements Command<FormInstance>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String formModelId;
    protected FormInfo formInfo;
    protected Map<String, Object> variables;
    protected String taskId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;

    public AbstractSaveFormInstanceCmd(FormInfo formInfo, Map<String, Object> variables, String taskId, String processInstanceId, String processDefinitionId) {
        this.formInfo = formInfo;
        this.variables = variables;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
    }

    public AbstractSaveFormInstanceCmd(String formModelId, Map<String, Object> variables, String taskId, String processInstanceId, String processDefinitionId) {
        this.formModelId = formModelId;
        this.variables = variables;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
        this.processDefinitionId = processDefinitionId;
    }
    
    public AbstractSaveFormInstanceCmd(String formModelId, Map<String, Object> variables, String taskId, String scopeId, String scopeType, String scopeDefinitionId) {
        this.formModelId = formModelId;
        this.variables = variables;
        this.taskId = taskId;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.scopeDefinitionId = scopeDefinitionId;
    }
    
    public AbstractSaveFormInstanceCmd(FormInfo formInfo, Map<String, Object> variables, String taskId, String scopeId, String scopeType, String scopeDefinitionId) {
        this.formInfo = formInfo;
        this.variables = variables;
        this.taskId = taskId;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public FormInstance execute(CommandContext commandContext) {

        if (formInfo == null) {
            if (formModelId == null) {
                throw new FlowableException("Invalid form model and no form model Id provided");
            }
            formInfo = CommandContextUtil.getFormEngineConfiguration().getFormRepositoryService().getFormModelById(formModelId);
        }

        if (formInfo == null || formInfo.getId() == null) {
            throw new FlowableException("Invalid form model provided");
        }

        ObjectMapper objectMapper = CommandContextUtil.getFormEngineConfiguration().getObjectMapper();
        ObjectNode submittedFormValuesJson = objectMapper.createObjectNode();

        ObjectNode valuesNode = submittedFormValuesJson.putObject("values");

        // Loop over all form fields and see if a value was provided
        
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        Map<String, FormField> fieldMap = formModel.allFieldsAsMap();
        for (String fieldId : fieldMap.keySet()) {
            FormField formField = fieldMap.get(fieldId);

            if (FormFieldTypes.EXPRESSION.equals(formField.getType()) || FormFieldTypes.CONTAINER.equals(formField.getType())) {
                continue;
            }

            if (variables.containsKey(fieldId)) {
                Object variableValue = variables.get(fieldId);
                if (variableValue == null) {
                    valuesNode.putNull(fieldId);
                } else if (variableValue instanceof Long) {
                    valuesNode.put(fieldId, (Long) variables.get(fieldId));

                } else if (variableValue instanceof Double) {
                    valuesNode.put(fieldId, (Double) variables.get(fieldId));
                    
                } else if (variableValue instanceof Boolean) {
                    valuesNode.put(fieldId, (Boolean) variables.get(fieldId));

                } else if (variableValue instanceof LocalDate) {
                    valuesNode.put(fieldId, ((LocalDate) variableValue).toString());

                } else {
                    valuesNode.put(fieldId, variableValue.toString());
                }
            }
        }

        // Handle outcome
        String outcomeVariable = null;
        if (formModel.getOutcomeVariableName() != null) {
            outcomeVariable = formModel.getOutcomeVariableName();
        } else {
            outcomeVariable = "form_" + formModel.getKey() + "_outcome";
        }

        if (variables.containsKey(outcomeVariable) && variables.get(outcomeVariable) != null) {
            submittedFormValuesJson.put("flowable_form_outcome", variables.get(outcomeVariable).toString());
        }

        FormInstanceEntityManager formInstanceEntityManager = CommandContextUtil.getFormInstanceEntityManager(commandContext);
        FormInstanceEntity formInstanceEntity = findExistingFormInstance(CommandContextUtil.getFormEngineConfiguration());

        if (formInstanceEntity == null) {
            formInstanceEntity = formInstanceEntityManager.create();
        }

        formInstanceEntity.setFormDefinitionId(formInfo.getId());
        formInstanceEntity.setTaskId(taskId);
        
        if (processInstanceId != null) {
            formInstanceEntity.setProcessInstanceId(processInstanceId);
            formInstanceEntity.setProcessDefinitionId(processDefinitionId);
            
        } else {
            formInstanceEntity.setScopeId(scopeId);
            formInstanceEntity.setScopeType(scopeType);
            formInstanceEntity.setScopeDefinitionId(scopeDefinitionId);
        }
        
        formInstanceEntity.setSubmittedDate(new Date());
        try {
            formInstanceEntity.setFormValueBytes(objectMapper.writeValueAsBytes(submittedFormValuesJson));
        } catch (Exception e) {
            throw new FlowableException("Error setting form values JSON", e);
        }

        if (formInstanceEntity.getId() == null) {
            formInstanceEntityManager.insert(formInstanceEntity);
        } else {
            formInstanceEntityManager.update(formInstanceEntity);
        }


        return formInstanceEntity;
    }

    protected abstract FormInstanceEntity findExistingFormInstance(FormEngineConfiguration formEngineConfiguration);
}