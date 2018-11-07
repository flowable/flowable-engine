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

public class GetDataObjectCmd implements Command<DataObject>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected String dataObjectName;
    protected boolean isLocal;
    protected String locale;
    protected boolean withLocalizationFallback;

    public GetDataObjectCmd(String executionId, String dataObjectName, boolean isLocal) {
        this.executionId = executionId;
        this.dataObjectName = dataObjectName;
        this.isLocal = isLocal;
    }

    public GetDataObjectCmd(String executionId, String dataObjectName, boolean isLocal, String locale, boolean withLocalizationFallback) {
        this.executionId = executionId;
        this.dataObjectName = dataObjectName;
        this.isLocal = isLocal;
        this.locale = locale;
        this.withLocalizationFallback = withLocalizationFallback;
    }

    @Override
    public DataObject execute(CommandContext commandContext) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("executionId is null");
        }
        if (dataObjectName == null) {
            throw new FlowableIllegalArgumentException("dataObjectName is null");
        }

        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);

        if (execution == null) {
            throw new FlowableObjectNotFoundException("execution " + executionId + " doesn't exist", Execution.class);
        }

        DataObject dataObject = null;

        VariableInstance variableEntity = null;
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            variableEntity = compatibilityHandler.getExecutionVariableInstance(executionId, dataObjectName, isLocal);

        } else {
            if (isLocal) {
                variableEntity = execution.getVariableInstanceLocal(dataObjectName, false);
            } else {
                variableEntity = execution.getVariableInstance(dataObjectName, false);
            }
        }

        String localizedName = null;
        String localizedDescription = null;

        if (variableEntity != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(variableEntity.getExecutionId());
            while (!executionEntity.isScope()) {
                executionEntity = executionEntity.getParent();
            }

            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(executionEntity.getProcessDefinitionId());
            ValuedDataObject foundDataObject = null;
            if (executionEntity.getParentId() == null) {
                for (ValuedDataObject dataObjectDefinition : bpmnModel.getMainProcess().getDataObjects()) {
                    if (dataObjectDefinition.getName().equals(variableEntity.getName())) {
                        foundDataObject = dataObjectDefinition;
                        break;
                    }
                }

            } else {
                SubProcess subProcess = (SubProcess) bpmnModel.getFlowElement(executionEntity.getActivityId());
                for (ValuedDataObject dataObjectDefinition : subProcess.getDataObjects()) {
                    if (dataObjectDefinition.getName().equals(variableEntity.getName())) {
                        foundDataObject = dataObjectDefinition;
                        break;
                    }
                }
            }

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
                dataObject = new DataObjectImpl(variableEntity.getId(), variableEntity.getProcessInstanceId(), variableEntity.getExecutionId(),
                        variableEntity.getName(), variableEntity.getValue(), foundDataObject.getDocumentation(),
                        foundDataObject.getType(), localizedName, localizedDescription, foundDataObject.getId());
            }
        }

        return dataObject;
    }
}
