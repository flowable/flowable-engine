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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.interceptor.Command;
import org.flowable.form.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntity;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormModel;
import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractSaveFormInstanceCmd implements Command<FormInstance>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String formModelId;
    protected FormModel formModel;
    protected Map<String, Object> variables;
    protected String taskId;
    protected String processInstanceId;

    public AbstractSaveFormInstanceCmd(FormModel formModel, Map<String, Object> variables, String taskId, String processInstanceId) {
        this.formModel = formModel;
        this.variables = variables;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
    }

    public AbstractSaveFormInstanceCmd(String formModelId, Map<String, Object> variables, String taskId, String processInstanceId) {
        this.formModelId = formModelId;
        this.variables = variables;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
    }

    public FormInstance execute(CommandContext commandContext) {

        if (formModel == null) {
            if (formModelId == null) {
                throw new FlowableException("Invalid form model and no form model Id provided");
            }
            formModel = commandContext.getFormEngineConfiguration().getFormRepositoryService().getFormModelById(formModelId);
        }

        if (formModel == null || formModel.getId() == null) {
            throw new FlowableException("Invalid form model provided");
        }

        ObjectMapper objectMapper = commandContext.getFormEngineConfiguration().getObjectMapper();
        ObjectNode submittedFormValuesJson = objectMapper.createObjectNode();

        ObjectNode valuesNode = submittedFormValuesJson.putObject("values");

        // Loop over all form fields and see if a value was provided
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

        FormInstanceEntityManager formInstanceEntityManager = commandContext.getFormInstanceEntityManager();
        FormInstanceEntity formInstanceEntity = findExistingFormInstance(commandContext.getFormEngineConfiguration());

        if (formInstanceEntity == null) {
            formInstanceEntity = formInstanceEntityManager.create();
        }

        formInstanceEntity.setFormDefinitionId(formModel.getId());
        formInstanceEntity.setTaskId(taskId);
        formInstanceEntity.setProcessInstanceId(processInstanceId);
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