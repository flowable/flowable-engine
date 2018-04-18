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

import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

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
    protected String processInstanceName;
    protected String callbackId;
    protected String callbackType;
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
        this.transientVariables = processInstanceBuilder.getTransientVariables();
        this.callbackId = processInstanceBuilder.getCallbackId();
        this.callbackType = processInstanceBuilder.getCallbackType();
    }

    @Override
    public ProcessInstance execute(CommandContext commandContext) {
        DeploymentManager deploymentCache = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager();

        // Find the process definition
        ProcessDefinition processDefinition = null;
        if (processDefinitionId != null) {

            processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for id = '" + processDefinitionId + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId))) {

            processDefinition = deploymentCache.findDeployedLatestProcessDefinitionByKey(processDefinitionKey);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey + "'", ProcessDefinition.class);
            }

        } else if (processDefinitionKey != null && tenantId != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {

            processDefinition = deploymentCache.findDeployedLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("No process definition found for key '" + processDefinitionKey + "' for tenant identifier " + tenantId, ProcessDefinition.class);
            }

        } else {
            throw new FlowableIllegalArgumentException("processDefinitionKey and processDefinitionId are null");
        }

        processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        ProcessInstance processInstance = createAndStartProcessInstance(processDefinition, businessKey, processInstanceName, 
                variables, transientVariables, callbackId, callbackType);

        return processInstance;
    }

    protected ProcessInstance createAndStartProcessInstance(ProcessDefinition processDefinition, String businessKey, String processInstanceName, 
                    Map<String, Object> variables, Map<String, Object> transientVariables, String callbackId, String callbackType) {
        
        return processInstanceHelper.createProcessInstance(processDefinition, businessKey, processInstanceName, variables, 
                        transientVariables, callbackId, callbackType, true);
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
