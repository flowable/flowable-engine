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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class StartProcessInstanceCmd<T> implements Command<ProcessInstance>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionKey;
    protected String processDefinitionId;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected String businessKey;
    protected String tenantId;
    protected String overrideDefinitionTenantId;
    protected String predefinedProcessInstanceId;
    protected String processInstanceName;
    protected String callbackId;
    protected String callbackType;
    protected Map<String, Object> startFormVariables;
    protected String outcome;
    protected boolean fallbackToDefaultTenant;
    protected ProcessInstanceHelper processInstanceHelper;

    public StartProcessInstanceCmd(String processDefinitionKey, String processDefinitionId, String businessKey, Map<String, Object> variables) {
        this.processDefinitionKey = processDefinitionKey;
        this.processDefinitionId = processDefinitionId;
        this.businessKey = businessKey;
        this.variables = variables;
    }

    public StartProcessInstanceCmd(String processDefinitionKey, String processDefinitionId, String businessKey, Map<String, Object> variables, String tenantId) {
        this(processDefinitionKey, processDefinitionId, businessKey, variables);
        this.tenantId = tenantId;
    }

    public StartProcessInstanceCmd(ProcessInstanceBuilderImpl processInstanceBuilder) {
        this(processInstanceBuilder.getProcessDefinitionKey(),
                processInstanceBuilder.getProcessDefinitionId(),
                processInstanceBuilder.getBusinessKey(),
                processInstanceBuilder.getVariables(),
                processInstanceBuilder.getTenantId());
        
        this.processInstanceName = processInstanceBuilder.getProcessInstanceName();
        this.overrideDefinitionTenantId = processInstanceBuilder.getOverrideDefinitionTenantId();
        this.predefinedProcessInstanceId = processInstanceBuilder.getPredefinedProcessInstanceId();
        this.transientVariables = processInstanceBuilder.getTransientVariables();
        this.callbackId = processInstanceBuilder.getCallbackId();
        this.callbackType = processInstanceBuilder.getCallbackType();
        this.startFormVariables = processInstanceBuilder.getStartFormVariables();
        this.outcome = processInstanceBuilder.getOutcome();
        this.fallbackToDefaultTenant = processInstanceBuilder.isFallbackToDefaultTenant();
    }

    @Override
    public ProcessInstance execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();
        ProcessDefinition processDefinition = getProcessDefinition(processEngineConfiguration);

        ProcessInstance processInstance = null;
        if (hasStartFormData()) {
            processInstance = handleProcessInstanceWithForm(commandContext, processDefinition, processEngineConfiguration);
        } else {
            processInstance = startProcessInstance(processDefinition);
        }

        return processInstance;
    }

    protected ProcessInstance handleProcessInstanceWithForm(CommandContext commandContext, ProcessDefinition processDefinition, 
                    ProcessEngineConfigurationImpl processEngineConfiguration) {
        FormInfo formInfo = null;
        Map<String, Object> formVariables = null;

        if (hasStartFormData()) {

            FormService formService = CommandContextUtil.getFormService(commandContext);
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            Process process = bpmnModel.getProcessById(processDefinition.getKey());
            FlowElement startElement = process.getInitialFlowElement();

            if (startElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) startElement;
                if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
                    FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService(commandContext);

                    if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                        formInfo = formRepositoryService.getFormModelByKey(startEvent.getFormKey());
                    } else {
                        formInfo = formRepositoryService.getFormModelByKey(startEvent.getFormKey(), tenantId, processEngineConfiguration.isFallbackToDefaultTenant());
                    }

                    if (formInfo != null) {
                        formVariables = formService.getVariablesFromFormSubmission(formInfo, startFormVariables, outcome);
                        processEngineConfiguration.getFormFieldHandler().validateFormFieldsOnSubmit(
                            formInfo, null, startFormVariables
                        );
                        if (formVariables != null) {
                            if (variables == null) {
                                variables = new HashMap<>();
                            }
                            variables.putAll(formVariables);
                        }
                    }
                }
            }

        }

        ProcessInstance processInstance = startProcessInstance(processDefinition);

        if (formInfo != null) {
            FormService formService = CommandContextUtil.getFormService(commandContext);
            formService.createFormInstance(formVariables, formInfo, null, processInstance.getId(), 
                            processInstance.getProcessDefinitionId(), processInstance.getTenantId());
            FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
            formFieldHandler.handleFormFieldsOnSubmit(formInfo, null, processInstance.getId(), null, null, variables, processInstance.getTenantId());
        }

        return processInstance;
    }

    protected ProcessInstance startProcessInstance(ProcessDefinition processDefinition) {
        return processInstanceHelper.createProcessInstance(processDefinition, businessKey, processInstanceName,
                            overrideDefinitionTenantId, predefinedProcessInstanceId, variables, transientVariables, callbackId, callbackType, true);
    }

    protected boolean hasStartFormData() {
        return startFormVariables != null || outcome != null;
    }

    protected ProcessDefinition getProcessDefinition(ProcessEngineConfigurationImpl processEngineConfiguration) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = processEngineConfiguration.getProcessDefinitionEntityManager();

        // Find the process definition
        ProcessDefinition processDefinition = null;
        if (processDefinitionId != null) {
            processDefinition = processDefinitionEntityManager.findById(processDefinitionId);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for id = '" + processDefinitionId + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId))) {

            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            if (processDefinition == null) {
                if (fallbackToDefaultTenant || processEngineConfiguration.isFallbackToDefaultTenant()) {
                    if (StringUtils.isNotEmpty(processEngineConfiguration.getDefaultTenantValue())) {
                        processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, 
                                        processEngineConfiguration.getDefaultTenantValue());
                        if (processDefinition != null) {
                            overrideDefinitionTenantId = tenantId;
                        }
                        
                    } else {
                        processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
                    }
                    
                    if (processDefinition == null) {
                        throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey +
                            "'. Fallback to default tenant was also applied.", ProcessDefinition.class);
                    }
                } else {
                    throw new FlowableObjectNotFoundException("Process definition with key '" + processDefinitionKey +
                        "' and tenantId '"+ tenantId +"' was not found", ProcessDefinition.class);
                }
            }

        } else {
            throw new FlowableIllegalArgumentException("processDefinitionKey and processDefinitionId are null");
        }
        return processDefinition;
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
}
