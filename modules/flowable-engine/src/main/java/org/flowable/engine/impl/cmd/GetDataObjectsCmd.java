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
import java.util.Map.Entry;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.DataObjectImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetDataObjectsCmd implements Command<Map<String, DataObject>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected Collection<String> dataObjectNames;
    protected boolean isLocal;
    protected String locale;
    protected boolean withLocalizationFallback;

    public GetDataObjectsCmd(String executionId, Collection<String> dataObjectNames, boolean isLocal) {
        this.executionId = executionId;
        this.dataObjectNames = dataObjectNames;
        this.isLocal = isLocal;
    }

    public GetDataObjectsCmd(String executionId, Collection<String> dataObjectNames, boolean isLocal, String locale, boolean withLocalizationFallback) {
        this.executionId = executionId;
        this.dataObjectNames = dataObjectNames;
        this.isLocal = isLocal;
        this.locale = locale;
        this.withLocalizationFallback = withLocalizationFallback;
    }

    @Override
    public Map<String, DataObject> execute(CommandContext commandContext) {

        // Verify existence of execution
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("executionId is null");
        }

        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);

        if (execution == null) {
            throw new FlowableObjectNotFoundException("execution " + executionId + " doesn't exist", Execution.class);
        }

        Map<String, VariableInstance> variables = null;

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            variables = compatibilityHandler.getExecutionVariableInstances(executionId, dataObjectNames, isLocal);

        } else {

            if (dataObjectNames == null || dataObjectNames.isEmpty()) {
                // Fetch all
                if (isLocal) {
                    variables = execution.getVariableInstancesLocal();
                } else {
                    variables = execution.getVariableInstances();
                }

            } else {
                // Fetch specific collection of variables
                if (isLocal) {
                    variables = execution.getVariableInstancesLocal(dataObjectNames, false);
                } else {
                    variables = execution.getVariableInstances(dataObjectNames, false);
                }
            }
        }

        Map<String, DataObject> dataObjects = null;
        if (variables != null) {
            dataObjects = new HashMap<>(variables.size());

            for (Entry<String, VariableInstance> entry : variables.entrySet()) {
                String name = entry.getKey();
                VariableInstance variableEntity = entry.getValue();

                ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(variableEntity.getExecutionId());
                while (!executionEntity.isScope()) {
                    executionEntity = executionEntity.getParent();
                }

                BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
                ValuedDataObject foundDataObject = null;
                if (executionEntity.getParentId() == null) {
                    for (ValuedDataObject dataObject : bpmnModel.getMainProcess().getDataObjects()) {
                        if (dataObject.getName().equals(variableEntity.getName())) {
                            foundDataObject = dataObject;
                            break;
                        }
                    }
                } else {
                    SubProcess subProcess = (SubProcess) bpmnModel.getFlowElement(executionEntity.getActivityId());
                    for (ValuedDataObject dataObject : subProcess.getDataObjects()) {
                        if (dataObject.getName().equals(variableEntity.getName())) {
                            foundDataObject = dataObject;
                            break;
                        }
                    }
                }

                String localizedName = null;
                String localizedDescription = null;

                if (locale != null && foundDataObject != null) {
                    ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, foundDataObject.getId(),
                            execution.getProcessDefinitionId(), withLocalizationFallback);

                    if (languageNode != null) {
                        JsonNode nameNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_NAME);
                        if (nameNode != null) {
                            localizedName = nameNode.asText();
                        }
                        JsonNode descriptionNode = languageNode.get(DynamicBpmnConstants.LOCALIZATION_DESCRIPTION);
                        if (descriptionNode != null) {
                            localizedDescription = descriptionNode.asText();
                        }
                    }
                }

                if (foundDataObject != null) {
                    dataObjects.put(name, new DataObjectImpl(variableEntity.getId(), variableEntity.getProcessInstanceId(),
                            variableEntity.getExecutionId(), variableEntity.getName(), variableEntity.getValue(),
                            foundDataObject.getDocumentation(), foundDataObject.getType(), localizedName,
                            localizedDescription, foundDataObject.getId()));
                }
            }
        }

        return dataObjects;
    }
}
