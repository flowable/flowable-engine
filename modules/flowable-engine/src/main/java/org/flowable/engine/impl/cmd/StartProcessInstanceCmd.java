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
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class StartProcessInstanceCmd<T> implements Command<ProcessInstance>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionKey;
    protected String processDefinitionId;
    protected String processDefinitionParentDeploymentId;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    protected String businessKey;
    protected String businessStatus;
    protected String tenantId;
    protected String overrideDefinitionTenantId;
    protected String predefinedProcessInstanceId;
    protected String processInstanceName;
    protected String startEventId;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
    protected String ownerId;
    protected String assigneeId;
    protected String stageInstanceId;
    protected Map<String, Object> startFormVariables;
    protected String outcome;
    protected Map<String, Object> extraFormVariables;
    protected FormInfo extraFormInfo;
    protected String extraFormOutcome;
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
        
        this.processDefinitionParentDeploymentId = processInstanceBuilder.getProcessDefinitionParentDeploymentId();
        this.processInstanceName = processInstanceBuilder.getProcessInstanceName();
        this.startEventId = processInstanceBuilder.getStartEventId();
        this.overrideDefinitionTenantId = processInstanceBuilder.getOverrideDefinitionTenantId();
        this.predefinedProcessInstanceId = processInstanceBuilder.getPredefinedProcessInstanceId();
        this.transientVariables = processInstanceBuilder.getTransientVariables();
        this.callbackId = processInstanceBuilder.getCallbackId();
        this.callbackType = processInstanceBuilder.getCallbackType();
        this.referenceId = processInstanceBuilder.getReferenceId();
        this.referenceType = processInstanceBuilder.getReferenceType();
        this.ownerId = processInstanceBuilder.getOwnerId();
        this.assigneeId = processInstanceBuilder.getAssigneeId();
        this.stageInstanceId = processInstanceBuilder.getStageInstanceId();
        this.startFormVariables = processInstanceBuilder.getStartFormVariables();
        this.outcome = processInstanceBuilder.getOutcome();
        this.extraFormVariables = processInstanceBuilder.getExtraFormVariables();
        this.extraFormInfo = processInstanceBuilder.getExtraFormInfo();
        this.extraFormOutcome = processInstanceBuilder.getExtraFormOutcome();
        this.fallbackToDefaultTenant = processInstanceBuilder.isFallbackToDefaultTenant();
        this.businessStatus = processInstanceBuilder.getBusinessStatus();
    }

    @Override
    public ProcessInstance execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        processInstanceHelper = processEngineConfiguration.getProcessInstanceHelper();
        ProcessDefinition processDefinition = getProcessDefinition(processEngineConfiguration, commandContext);

        ProcessInstance processInstance = null;
        if (hasFormData()) {
            processInstance = handleProcessInstanceWithForm(commandContext, processDefinition, processEngineConfiguration);
        } else {
            processInstance = startProcessInstance(processDefinition);
        }

        return processInstance;
    }

    protected ProcessInstance handleProcessInstanceWithForm(CommandContext commandContext, ProcessDefinition processDefinition, 
                    ProcessEngineConfigurationImpl processEngineConfiguration) {
        
        FormService formService = CommandContextUtil.getFormService(commandContext);
        
        FlowElement startElement = null;
        if (hasStartFormData() || extraFormInfo != null) {
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinition.getId());
            Process process = bpmnModel.getProcessById(processDefinition.getKey());
            startElement = process.getInitialFlowElement();
        }

        FormInfo formInfo = null;
        Map<String, Object> processVariables = null;
        if (hasStartFormData()) {
            if (startElement instanceof StartEvent startEvent) {
                String startFormKey = startEvent.getFormKey();
                if (StringUtils.isNotEmpty(startFormKey)) {
                    FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService(commandContext);

                    formInfo = resolveFormInfo(startEvent, processDefinition, formRepositoryService, processEngineConfiguration);

                    if (formInfo != null) {
                        if (isFormFieldValidationEnabled(processEngineConfiguration, startEvent)) {
                            formService.validateFormFields(startEvent.getId(), "startEvent", null, processDefinition.getId(), 
                                    ScopeTypes.BPMN, formInfo, startFormVariables);
                        }
                        // The processVariables are the variables that should be used when starting the process
                        // the actual variables should instead be used when saving the form instances
                        processVariables = formService.getVariablesFromFormSubmission(startElement.getId(), "startEvent", 
                                null, processDefinition.getId(), ScopeTypes.BPMN, formInfo, startFormVariables, outcome);
                        if (processVariables != null) {
                            if (variables == null) {
                                variables = new HashMap<>();
                            }
                            variables.putAll(processVariables);
                        }
                    }
                }
            }

        }

        Map<String, Object> extraFormVariables = null;
        if (extraFormInfo != null) {
            String startEventId = null;
            if (startElement instanceof StartEvent) {
                startEventId = startElement.getId();
            }
            
            extraFormVariables = formService.getVariablesFromFormSubmission(startEventId, "startEvent", null, processDefinition.getId(), 
                    ScopeTypes.BPMN, this.extraFormInfo, this.extraFormVariables, this.extraFormOutcome);

            if (extraFormVariables != null) {
                if (variables == null) {
                    variables = new HashMap<>();
                }

                variables.putAll(extraFormVariables);
            }
        }

        ProcessInstance processInstance = startProcessInstance(processDefinition);

        if (processVariables != null) {
            // processVariables can be non null only if the formInfo was not null
            formService.createFormInstance(startFormVariables, formInfo, null, processInstance.getId(),
                            processInstance.getProcessDefinitionId(), processInstance.getTenantId(), outcome);
            FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
            formFieldHandler.handleFormFieldsOnSubmit(formInfo, null, processInstance.getId(), null, null, processVariables, processInstance.getTenantId());
        }

        if (extraFormVariables != null) {
            // extraFormVariables can be non null only if the extraFormInfo was not null
            FormFieldHandler formFieldHandler = processEngineConfiguration.getFormFieldHandler();
            formFieldHandler.handleFormFieldsOnSubmit(extraFormInfo, null, processInstance.getId(), null, null, extraFormVariables, processInstance.getTenantId());
        }

        return processInstance;
    }

    protected FormInfo resolveFormInfo(StartEvent startEvent, ProcessDefinition processDefinition, FormRepositoryService formRepositoryService,
            ProcessEngineConfigurationImpl processEngineConfiguration) {
        String formKey = startEvent.getFormKey();
        FormInfo formInfo;
        if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            if (startEvent.isSameDeployment()) {
                String parentDeploymentId = ProcessDefinitionUtil.getDefinitionDeploymentId(processDefinition, processEngineConfiguration);
                formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(formKey, parentDeploymentId);
            } else {
                formInfo = formRepositoryService.getFormModelByKey(formKey);
            }
        } else {
            if (startEvent.isSameDeployment()) {
                String parentDeploymentId = ProcessDefinitionUtil.getDefinitionDeploymentId(processDefinition, processEngineConfiguration);
                formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(formKey, parentDeploymentId, tenantId,
                        processEngineConfiguration.isFallbackToDefaultTenant());
            } else {
                formInfo = formRepositoryService.getFormModelByKey(formKey, tenantId, processEngineConfiguration.isFallbackToDefaultTenant());
            }
        }

        return formInfo;
    }
    protected boolean isFormFieldValidationEnabled(ProcessEngineConfigurationImpl processEngineConfiguration, StartEvent startEvent) {
        if (processEngineConfiguration.isFormFieldValidationEnabled()) {
            return TaskHelper.isFormFieldValidationEnabled(NoExecutionVariableScope.getSharedInstance() // process instance does not exist yet
                , processEngineConfiguration, startEvent.getValidateFormFields());
        }
        return false;
    }

    protected ProcessInstance startProcessInstance(ProcessDefinition processDefinition) {
        return processInstanceHelper.createProcessInstance(processDefinition, businessKey, businessStatus, processInstanceName,
            startEventId, overrideDefinitionTenantId, predefinedProcessInstanceId, variables, transientVariables,
            callbackId, callbackType, referenceId, referenceType, ownerId, assigneeId, stageInstanceId, true);
    }

    protected boolean hasStartFormData() {
        return startFormVariables != null || outcome != null;
    }

    protected boolean hasFormData() {
        return hasStartFormData() || extraFormInfo != null;
    }

    protected ProcessDefinition getProcessDefinition(ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {
        DeploymentManager deploymentCache = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager();
        ProcessDefinitionEntityManager processDefinitionEntityManager = processEngineConfiguration.getProcessDefinitionEntityManager();

        // Find the process definition
        ProcessDefinition processDefinition = null;
        if (processDefinitionId != null) {
            processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for id = '" + processDefinitionId + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId))) {

            if (processDefinitionParentDeploymentId != null) {
                processDefinition = processDefinitionEntityManager
                        .findProcessDefinitionByParentDeploymentAndKey(processDefinitionParentDeploymentId, processDefinitionKey);
            }

            if (processDefinition == null) {
                processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);
            }

            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {

            if (processDefinitionParentDeploymentId != null) {
                processDefinition = processDefinitionEntityManager
                        .findProcessDefinitionByParentDeploymentAndKeyAndTenantId(processDefinitionParentDeploymentId, processDefinitionKey, tenantId);
            }

            if (processDefinition == null) {
                processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            }

            if (processDefinition == null) {
                if (fallbackToDefaultTenant || processEngineConfiguration.isFallbackToDefaultTenant()) {
                    String defaultTenant = processEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.BPMN, processDefinitionKey);
                    if (StringUtils.isNotEmpty(defaultTenant)) {
                        processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, defaultTenant);
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
