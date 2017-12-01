/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.FormModel;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class StartProcessInstanceWithFormCmd implements Command<ProcessInstance>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;
    protected String outcome;
    protected Map<String, Object> variables;
    protected String processInstanceName;

    public StartProcessInstanceWithFormCmd(String processDefinitionId, String outcome, Map<String, Object> variables, String processInstanceName) {
        this.processDefinitionId = processDefinitionId;
        this.outcome = outcome;
        this.variables = variables;
        this.processInstanceName = processInstanceName;
    }

    @Override
    public ProcessInstance execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        DeploymentManager deploymentCache = processEngineConfiguration.getDeploymentManager();

        // Find the process definition
        ProcessDefinition processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("No process definition found for id = '" + processDefinitionId + "'", ProcessDefinition.class);
        }

        FormModel formModel = null;
        Map<String, Object> formVariables = null;
        FormService formService = CommandContextUtil.getFormService();

        if (variables != null || outcome != null) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            Process process = bpmnModel.getProcessById(processDefinition.getKey());
            FlowElement startElement = process.getInitialFlowElement();
            if (startElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) startElement;
                if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
                    FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
                    formModel = formRepositoryService.getFormModelByKey(startEvent.getFormKey());
                    if (formModel != null) {
                        formVariables = formService.getVariablesFromFormSubmission(formModel, variables, outcome);
                    }
                }
            }
        }

        ProcessInstance processInstance = createAndStartProcessInstance(processDefinition, processInstanceName,
                formVariables, commandContext);

        if (formModel != null) {
            formService.createFormInstance(formVariables, formModel, null, processInstance.getId(), processInstance.getProcessDefinitionId());

            processUploadFieldsIfNeeded(formModel, processInstance.getId());
        }

        return processInstance;
    }

    protected ProcessInstance createAndStartProcessInstance(ProcessDefinition processDefinition, String processInstanceName,
            Map<String, Object> variables, CommandContext commandContext) {

        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        return processInstanceHelper.createAndStartProcessInstance(processDefinition, null, processInstanceName, variables, null);
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }

    /**
     * When content is uploaded for a field, it is uploaded as a 'temporary related content'. Now that the task is completed, we need to associate the field/taskId/processInstanceId with the related
     * content so we can retrieve it later.
     */
    protected void processUploadFieldsIfNeeded(FormModel formModel, String processInstanceId) {
        ContentService contentService = CommandContextUtil.getContentService();
        if (contentService == null) {
            return;
        }

        if (formModel != null && formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    String variableName = formField.getId();
                    if (variables.containsKey(variableName)) {
                        String variableValue = (String) variables.get(variableName);
                        if (StringUtils.isNotEmpty(variableValue)) {
                            String[] contentItemIds = StringUtils.split(variableValue, ",");
                            Set<String> contentItemIdSet = new HashSet<>();
                            Collections.addAll(contentItemIdSet, contentItemIds);

                            List<ContentItem> contentItems = contentService.createContentItemQuery().ids(contentItemIdSet).list();

                            for (ContentItem contentItem : contentItems) {
                                contentItem.setProcessInstanceId(processInstanceId);
                                contentItem.setField(formField.getId());
                                contentService.saveContentItem(contentItem);
                            }
                        }
                    }
                }
            }
        }
    }
}
