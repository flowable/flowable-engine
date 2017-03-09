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

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.common.impl.util.io.BytesStreamSource;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;

public class InjectParallelEmbeddedSubProcessCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String taskId;
    protected DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder;

    public InjectParallelEmbeddedSubProcessCmd(String taskId, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder) {
        this.taskId = taskId;
        this.dynamicEmbeddedSubProcessBuilder = dynamicEmbeddedSubProcessBuilder;
        if (this.dynamicEmbeddedSubProcessBuilder.getId() == null) {
            this.dynamicEmbeddedSubProcessBuilder.setId(UUID.randomUUID().toString());
        }
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForTask(commandContext, taskId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
            ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {

        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        FlowElement taskFlowElement = process.getFlowElement(taskEntity.getTaskDefinitionKey(), true);
        if (taskFlowElement == null || !(taskFlowElement instanceof UserTask)) {
            throw new FlowableException("No UserTask instance found for task definition key " + taskEntity.getTaskDefinitionKey());
        }
        UserTask userTask = (UserTask) taskFlowElement;

        if (process.getFlowElement(dynamicEmbeddedSubProcessBuilder.getId(), true) != null) {
            throw new FlowableIllegalArgumentException("Invalid sub-process identifier: identifier already exists in host process definition");
        }

        SubProcess subProcess = new SubProcess();
        subProcess.setId(dynamicEmbeddedSubProcessBuilder.getId());

        ProcessDefinition subProcessDefinition = ProcessDefinitionUtil.getProcessDefinition(dynamicEmbeddedSubProcessBuilder.getProcessDefinitionId());
        ResourceEntity subProcessBpmnResource = commandContext.getResourceEntityManager()
                .findResourceByDeploymentIdAndResourceName(subProcessDefinition.getDeploymentId(), subProcessDefinition.getResourceName());
        BpmnModel bpmnModelSubProcess = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(subProcessBpmnResource.getBytes()), false, false);
        for (FlowElement flowElement : bpmnModelSubProcess.getProcesses().get(0).getFlowElements()) {
            subProcess.addFlowElement(flowElement);
        }
        processSubProcessFlowElements(commandContext, process, subProcess, originalProcessDefinitionEntity, newDeploymentEntity);

        FlowElementsContainer parentContainer = userTask.getParentContainer();
        parentContainer.addFlowElement(subProcess);

        ParallelGateway fork = new ParallelGateway();
        fork.setId("fork-" + UUID.randomUUID().toString());
        parentContainer.addFlowElement(fork);
        userTask.getIncomingFlows().get(0).setTargetRef(fork.getId());

        SequenceFlow forkFlow1 = new SequenceFlow(fork.getId(), userTask.getId());
        forkFlow1.setId("flow-" + UUID.randomUUID().toString());
        parentContainer.addFlowElement(forkFlow1);

        SequenceFlow forkFlow2 = new SequenceFlow(fork.getId(), subProcess.getId());
        forkFlow2.setId("flow-" +UUID.randomUUID().toString());
        parentContainer.addFlowElement(forkFlow2);

        if (dynamicEmbeddedSubProcessBuilder.isJoinParallelActivitiesOnComplete()) {
            ParallelGateway join = new ParallelGateway();
            join.setId("join-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(join);

            SequenceFlow joinFlow1 = new SequenceFlow(userTask.getId(), join.getId());
            joinFlow1.setId("flow-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(joinFlow1);

            SequenceFlow joinFlow2 = new SequenceFlow(subProcess.getId(), join.getId());
            joinFlow2.setId("flow-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(joinFlow2);

            SequenceFlow outgoingFlow = userTask.getOutgoingFlows().get(0);
            outgoingFlow.setSourceRef(join.getId());

        } else {
            EndEvent endEvent = new EndEvent();
            endEvent.setId("end" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(endEvent);

            SequenceFlow endFlow = new SequenceFlow(subProcess.getId(), endEvent.getId());
            endFlow.setId("flow" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(endFlow);

        }

    }

    protected void processSubProcessFlowElements(CommandContext commandContext, Process process, SubProcess subProcess, 
            ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        
        Collection<FlowElement> flowElementsOfSubProcess = subProcess.getFlowElementMap().values(); 
        for (FlowElement flowElement : flowElementsOfSubProcess) {

            if (process.getFlowElement(flowElement.getId(), true) != null) {
                generateIdForDuplicateFlowElement(process, flowElement);
            }

            if (flowElement instanceof UserTask && commandContext.getProcessEngineConfiguration().isFormEngineInitialized()) {
                UserTask userTask = (UserTask) flowElement;
                if (StringUtils.isNotEmpty(userTask.getFormKey())) {
                    FormRepositoryService formRepositoryService = commandContext.getProcessEngineConfiguration().getFormEngineRepositoryService();
                    FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery()
                            .formDefinitionKey(userTask.getFormKey()).parentDeploymentId(originalProcessDefinitionEntity.getDeploymentId()).latestVersion().singleResult();
                    if (formDefinition != null) {
                        String name = formDefinition.getResourceName();
                        InputStream inputStream = formRepositoryService.getResourceAsStream(formDefinition.getId(), name);
                        addResource(commandContext, newDeploymentEntity, name, IoUtil.readInputStream(inputStream, name));
                        IoUtil.closeSilently(inputStream);
                    }
                }
            }

            if (flowElement instanceof ServiceTask 
                    && ServiceTask.DMN_TASK.equals(((ServiceTask) flowElement).getType()) 
                    && commandContext.getProcessEngineConfiguration().isDmnEngineInitialized()) {
                
                ServiceTask serviceTask = (ServiceTask) flowElement;
                if (serviceTask.getFieldExtensions() != null && serviceTask.getFieldExtensions().size() > 0) {
                    String decisionTableReferenceKey = null;
                    for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
                        if ("decisionTableReferenceKey".equals(fieldExtension.getFieldName())) {
                            decisionTableReferenceKey = fieldExtension.getStringValue();
                            break;
                        }
                    }

                    if (decisionTableReferenceKey != null) {
                        DmnRepositoryService dmnRepositoryService = commandContext.getProcessEngineConfiguration().getDmnEngineRepositoryService();
                        DmnDecisionTable dmnDecisionTable = dmnRepositoryService.createDecisionTableQuery()
                                .decisionTableKey(decisionTableReferenceKey).parentDeploymentId(originalProcessDefinitionEntity.getDeploymentId()).latestVersion().singleResult();
                        if (dmnDecisionTable != null) {
                            String name = dmnDecisionTable.getResourceName();
                            InputStream inputStream = dmnRepositoryService.getDmnResource(dmnDecisionTable.getId());
                            addResource(commandContext, newDeploymentEntity, name, IoUtil.readInputStream(inputStream, name));
                            IoUtil.closeSilently(inputStream);
                        }
                    }
                }
            }
        }
    }

    protected void generateIdForDuplicateFlowElement(org.flowable.bpmn.model.Process process, FlowElement duplicateFlowElement) {
        String originalFlowElementId = duplicateFlowElement.getId();
        if (process.getFlowElement(originalFlowElementId, true) != null) {
            String prefix = dynamicEmbeddedSubProcessBuilder.getId();
            String newFlowElementId = prefix + "-" + originalFlowElementId;
            int counter = 0;
            boolean maxLengthReached = false;
            while (!maxLengthReached && process.getFlowElement(newFlowElementId, true) != null) {
                newFlowElementId = prefix + (counter++) + "-" + originalFlowElementId;
                if (newFlowElementId.length() > 255) {
                    maxLengthReached = true;
                }
            }

            if (maxLengthReached) {
                newFlowElementId = prefix + "-" + UUID.randomUUID().toString();
            }

            duplicateFlowElement.setId(newFlowElementId);

            for (FlowElement flowElement : duplicateFlowElement.getParentContainer().getFlowElements()) {
                if (flowElement instanceof SequenceFlow) {
                    SequenceFlow sequenceFlow = (SequenceFlow) flowElement; 
                    if (sequenceFlow.getSourceRef().equals(originalFlowElementId)) {
                        sequenceFlow.setSourceRef(newFlowElementId);
                    }
                    if (sequenceFlow.getTargetRef().equals(originalFlowElementId)) {
                        sequenceFlow.setTargetRef(newFlowElementId);
                    }

                } else if (flowElement instanceof BoundaryEvent) {
                    BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
                    if (boundaryEvent.getAttachedToRefId().equals(originalFlowElementId)) {
                        boundaryEvent.setAttachedToRefId(newFlowElementId);
                    }
                    if (boundaryEvent.getEventDefinitions() != null 
                            && boundaryEvent.getEventDefinitions().size() > 0
                            && (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                        
                        CompensateEventDefinition compensateEventDefinition = (CompensateEventDefinition) boundaryEvent.getEventDefinitions().get(0);
                        if (compensateEventDefinition.getActivityRef().equals(originalFlowElementId)) {
                            compensateEventDefinition.setActivityRef(newFlowElementId);
                        }
                    }
                } 
            }
        }

        if (duplicateFlowElement instanceof FlowElementsContainer) {
            FlowElementsContainer flowElementsContainer = (FlowElementsContainer) duplicateFlowElement;
            for (FlowElement childFlowElement : flowElementsContainer.getFlowElements()) {
                generateIdForDuplicateFlowElement(process, childFlowElement);
            }
        }
    }

    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity,
            ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {

        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();

        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        ExecutionEntity executionAtTask = taskEntity.getExecution();

        ExecutionEntity execution = executionEntityManager.create();
        execution.setProcessInstanceId(processInstance.getId());
        execution.setParentId(executionAtTask.getParentId());
        execution.setProcessDefinitionId(processDefinitionEntity.getId());
        execution.setRootProcessInstanceId(processInstance.getRootProcessInstanceId());
        execution.setActive(true);
        execution.setScope(true);
        execution.setTenantId(processInstance.getTenantId());
        execution.setStartTime(processInstance.getStartTime());
        execution.setStartUserId(processInstance.getStartUserId());
        
        executionEntityManager.insert(execution);

        SubProcess subProcess = (SubProcess) ProcessDefinitionUtil
                .getBpmnModel(processDefinitionEntity.getId()).getMainProcess().getFlowElement(dynamicEmbeddedSubProcessBuilder.getId(), true);
        execution.setCurrentFlowElement(subProcess);

        Context.getAgenda().planContinueProcessOperation(execution);
    }

}