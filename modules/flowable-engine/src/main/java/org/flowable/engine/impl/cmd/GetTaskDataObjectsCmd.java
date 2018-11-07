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
import org.flowable.engine.impl.DataObjectImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.runtime.DataObject;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetTaskDataObjectsCmd implements Command<Map<String, DataObject>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String taskId;
    protected Collection<String> variableNames;
    protected String locale;
    protected boolean withLocalizationFallback;

    public GetTaskDataObjectsCmd(String taskId, Collection<String> variableNames) {
        this.taskId = taskId;
        this.variableNames = variableNames;
    }

    public GetTaskDataObjectsCmd(String taskId, Collection<String> variableNames, String locale, boolean withLocalizationFallback) {
        this.taskId = taskId;
        this.variableNames = variableNames;
        this.locale = locale;
        this.withLocalizationFallback = withLocalizationFallback;
    }

    @Override
    public Map<String, DataObject> execute(CommandContext commandContext) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }

        TaskEntity task = CommandContextUtil.getTaskService().getTask(taskId);

        if (task == null) {
            throw new FlowableObjectNotFoundException("task " + taskId + " doesn't exist", Task.class);
        }

        Map<String, DataObject> dataObjects = null;
        Map<String, VariableInstance> variables = null;
        if (variableNames == null) {
            variables = task.getVariableInstances();
        } else {
            variables = task.getVariableInstances(variableNames, false);
        }

        if (variables != null) {
            dataObjects = new HashMap<>(variables.size());

            for (Entry<String, VariableInstance> entry : variables.entrySet()) {
                VariableInstance variableEntity = entry.getValue();

                String localizedName = null;
                String localizedDescription = null;

                ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(variableEntity.getExecutionId());
                while (!executionEntity.isScope()) {
                    executionEntity = executionEntity.getParent();
                }

                BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(executionEntity.getProcessDefinitionId());
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

                if (locale != null && foundDataObject != null) {
                    ObjectNode languageNode = BpmnOverrideContext.getLocalizationElementProperties(locale, foundDataObject.getId(),
                            task.getProcessDefinitionId(), withLocalizationFallback);

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
                    dataObjects.put(
                            variableEntity.getName(), new DataObjectImpl(variableEntity.getId(), variableEntity.getProcessInstanceId(),
                                    variableEntity.getExecutionId(), variableEntity.getName(), variableEntity.getValue(),
                            foundDataObject.getDocumentation(), foundDataObject.getType(), localizedName, localizedDescription, foundDataObject.getId()));
                }
            }
        }

        return dataObjects;
    }
}
