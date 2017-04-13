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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
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
            BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {

        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        FlowElement taskFlowElement = process.getFlowElement(taskEntity.getTaskDefinitionKey(), true);
        if (taskFlowElement == null || !(taskFlowElement instanceof UserTask)) {
            throw new FlowableException("No UserTask instance found for task definition key " + taskEntity.getTaskDefinitionKey());
        }
        UserTask userTask = (UserTask) taskFlowElement;

        if (process.getFlowElement(dynamicEmbeddedSubProcessBuilder.getId(), true) != null) {
            throw new FlowableIllegalArgumentException("Invalid sub-process identifier: identifier already exists in host process definition");
        }
        
        SubProcess parentSubProcess = new SubProcess();
        parentSubProcess.setId("subProcess-" + userTask.getId());
        parentSubProcess.setName(userTask.getName());
        
        for (SequenceFlow incomingFlow : userTask.getIncomingFlows()) {
            incomingFlow.setTargetRef(parentSubProcess.getId());
        }
        parentSubProcess.setIncomingFlows(userTask.getIncomingFlows());
        
        for (SequenceFlow outgoingFlow : userTask.getOutgoingFlows()) {
            outgoingFlow.setSourceRef(parentSubProcess.getId());
        }
        parentSubProcess.setOutgoingFlows(userTask.getOutgoingFlows());
        
        userTask.setIncomingFlows(new ArrayList<SequenceFlow>());
        userTask.setOutgoingFlows(new ArrayList<SequenceFlow>());
        
        GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(userTask.getId());
        if (elementGraphicInfo != null) {
            elementGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(parentSubProcess.getId(), elementGraphicInfo);
        }
        
        FlowElementsContainer parentContainer = userTask.getParentContainer();
        
        parentContainer.removeFlowElement(userTask.getId());
        bpmnModel.removeGraphicInfo(userTask.getId());
        parentSubProcess.addFlowElement(userTask);
        
        parentContainer.addFlowElement(parentSubProcess);

        SubProcess subProcess = new SubProcess();
        subProcess.setId(dynamicEmbeddedSubProcessBuilder.getId());

        ProcessDefinition subProcessDefinition = ProcessDefinitionUtil.getProcessDefinition(dynamicEmbeddedSubProcessBuilder.getProcessDefinitionId());
        ResourceEntity subProcessBpmnResource = commandContext.getResourceEntityManager()
                .findResourceByDeploymentIdAndResourceName(subProcessDefinition.getDeploymentId(), subProcessDefinition.getResourceName());
        BpmnModel bpmnModelSubProcess = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(subProcessBpmnResource.getBytes()), false, false);
        for (FlowElement flowElement : bpmnModelSubProcess.getMainProcess().getFlowElements()) {
            subProcess.addFlowElement(flowElement);
        }
        
        Map<String, FlowElement> generatedIds = new HashMap<>();
        processSubProcessFlowElements(commandContext, process, bpmnModel, subProcess, bpmnModelSubProcess, 
                        originalProcessDefinitionEntity, newDeploymentEntity, generatedIds, (elementGraphicInfo != null));
        
        for (String originalFlowElementId : generatedIds.keySet()) {
            FlowElement duplicateFlowElement = generatedIds.get(originalFlowElementId);
            duplicateFlowElement.getParentContainer().removeFlowElementFromMap(originalFlowElementId);
            duplicateFlowElement.getParentContainer().addFlowElementToMap(duplicateFlowElement);
        }

        parentSubProcess.addFlowElement(subProcess);
        
        StartEvent startEvent = new StartEvent();
        startEvent.setId("start-" + UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(startEvent);

        ParallelGateway fork = new ParallelGateway();
        fork.setId("fork-" + UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(fork);
        
        SequenceFlow startFlow1 = new SequenceFlow(startEvent.getId(), fork.getId());
        startFlow1.setId("flow-" + UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(startFlow1);

        SequenceFlow forkFlow1 = new SequenceFlow(fork.getId(), userTask.getId());
        forkFlow1.setId("flow-" + UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(forkFlow1);

        SequenceFlow forkFlow2 = new SequenceFlow(fork.getId(), subProcess.getId());
        forkFlow2.setId("flow-" +UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(forkFlow2);
        
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end-" + UUID.randomUUID().toString());
        parentSubProcess.addFlowElement(endEvent);
        
        ParallelGateway join = null;
        SequenceFlow joinFlow1 = null;
        SequenceFlow joinFlow2 = null;
        SequenceFlow endFlow = null;

        if (dynamicEmbeddedSubProcessBuilder.isJoinParallelActivitiesOnComplete()) {
            join = new ParallelGateway();
            join.setId("join-" + UUID.randomUUID().toString());
            parentSubProcess.addFlowElement(join);

            joinFlow1 = new SequenceFlow(userTask.getId(), join.getId());
            joinFlow1.setId("flow-" + UUID.randomUUID().toString());
            parentSubProcess.addFlowElement(joinFlow1);

            joinFlow2 = new SequenceFlow(subProcess.getId(), join.getId());
            joinFlow2.setId("flow-" + UUID.randomUUID().toString());
            parentSubProcess.addFlowElement(joinFlow2);

            endFlow = new SequenceFlow(join.getId(), endEvent.getId());
            endFlow.setId("flow-" + UUID.randomUUID().toString());
            parentSubProcess.addFlowElement(endFlow);

        } else {
            endFlow = new SequenceFlow(subProcess.getId(), endEvent.getId());
            endFlow.setId("flow-" + UUID.randomUUID().toString());
            parentSubProcess.addFlowElement(endFlow);
        }

        if (elementGraphicInfo != null) {
            GraphicInfo startGraphicInfo = new GraphicInfo(45, 135, 30, 30);
            bpmnModel.addGraphicInfo(startEvent.getId(), startGraphicInfo);
            
            GraphicInfo forkGraphicInfo = new GraphicInfo(120, 130, 40, 40);
            bpmnModel.addGraphicInfo(fork.getId(), forkGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(startFlow1.getId(), createWayPoints(75, 150.093, 120.375, 150.375));
            
            GraphicInfo taskGraphicInfo = new GraphicInfo(205, 30, 80, 100);
            bpmnModel.addGraphicInfo(userTask.getId(), taskGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow1.getId(), createWayPoints(140.5, 130.5, 140.5, 70, 205, 70));
            
            GraphicInfo newSubProcessGraphicInfo = new GraphicInfo(205, 195, 80, 100);
            newSubProcessGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(subProcess.getId(), newSubProcessGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow2.getId(), createWayPoints(140.5, 169.5, 140.5, 235, 205, 235));
            
            if (join != null) {
                GraphicInfo joinGraphicInfo = new GraphicInfo(350, 130, 40, 40);
                bpmnModel.addGraphicInfo(join.getId(), joinGraphicInfo);
                
                bpmnModel.addFlowGraphicInfoList(joinFlow1.getId(), createWayPoints(305, 70, 370, 70, 370, 130));
                
                bpmnModel.addFlowGraphicInfoList(joinFlow2.getId(), createWayPoints(305, 235, 370, 235, 370, 169.5));
            }
            
            GraphicInfo endGraphicInfo = new GraphicInfo(435, 136, 28, 28);
            bpmnModel.addGraphicInfo(endEvent.getId(), endGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(endFlow.getId(), createWayPoints(389.621, 150.378, 435, 150.089));
        }
    }

    protected void processSubProcessFlowElements(CommandContext commandContext, Process process, BpmnModel bpmnModel, SubProcess subProcess, 
            BpmnModel subProcessBpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, 
            DeploymentEntity newDeploymentEntity, Map<String, FlowElement> generatedIds, boolean includeDiInfo) {
        
        Collection<FlowElement> flowElementsOfSubProcess = subProcess.getFlowElementMap().values(); 
        for (FlowElement flowElement : flowElementsOfSubProcess) {

            if (process.getFlowElement(flowElement.getId(), true) != null) {
                generateIdForDuplicateFlowElement(process, bpmnModel, subProcessBpmnModel, flowElement, generatedIds, includeDiInfo);
            } else {
                if (includeDiInfo) {
                    if (flowElement instanceof SequenceFlow) {
                        List<GraphicInfo> wayPoints = subProcessBpmnModel.getFlowLocationGraphicInfo(flowElement.getId());
                        if (wayPoints != null) {
                            bpmnModel.addFlowGraphicInfoList(flowElement.getId(), wayPoints);
                        }
                        
                    } else {
                        GraphicInfo graphicInfo = subProcessBpmnModel.getGraphicInfo(flowElement.getId());
                        if (graphicInfo != null) {
                            bpmnModel.addGraphicInfo(flowElement.getId(), subProcessBpmnModel.getGraphicInfo(flowElement.getId()));
                        }
                    }
                }
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
                
            } else if (flowElement instanceof ServiceTask 
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
                
            } else if (flowElement instanceof SubProcess) {
                processSubProcessFlowElements(commandContext, process, bpmnModel, (SubProcess) flowElement, 
                        subProcessBpmnModel, originalProcessDefinitionEntity, newDeploymentEntity, generatedIds, includeDiInfo);
            }
        }
    }

    protected void generateIdForDuplicateFlowElement(org.flowable.bpmn.model.Process process, BpmnModel bpmnModel, 
                    BpmnModel subProcessBpmnModel, FlowElement duplicateFlowElement, Map<String, FlowElement> generatedIds, boolean includeDiInfo) {
        
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
            generatedIds.put(originalFlowElementId, duplicateFlowElement);
            
            if (includeDiInfo) {
                if (duplicateFlowElement instanceof SequenceFlow) {
                    bpmnModel.addFlowGraphicInfoList(newFlowElementId, subProcessBpmnModel.getFlowLocationGraphicInfo(originalFlowElementId));
                    
                } else {
                    bpmnModel.addGraphicInfo(newFlowElementId, subProcessBpmnModel.getGraphicInfo(originalFlowElementId));
                }
            }

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
                generateIdForDuplicateFlowElement(process, bpmnModel, subProcessBpmnModel, childFlowElement, generatedIds, includeDiInfo);
            }
        }
    }

    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity,
            ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {

        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());
        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        ExecutionEntity executionAtTask = taskEntity.getExecution();
        
        FlowElement subProcessElement = bpmnModel.getFlowElement("subProcess-" + executionAtTask.getCurrentActivityId());
        ExecutionEntity subProcessExecution = executionEntityManager.create();
        subProcessExecution.setProcessInstanceId(processInstance.getId());
        subProcessExecution.setParentId(executionAtTask.getParentId());
        subProcessExecution.setProcessDefinitionId(processDefinitionEntity.getId());
        subProcessExecution.setRootProcessInstanceId(processInstance.getRootProcessInstanceId());
        subProcessExecution.setActive(true);
        subProcessExecution.setScope(true);
        subProcessExecution.setTenantId(processInstance.getTenantId());
        subProcessExecution.setStartTime(processInstance.getStartTime());
        subProcessExecution.setCurrentFlowElement(subProcessElement);
        executionEntityManager.insert(subProcessExecution);
        
        executionAtTask.setParent(subProcessExecution);

        ExecutionEntity execution = executionEntityManager.create();
        execution.setProcessInstanceId(processInstance.getId());
        execution.setParentId(subProcessExecution.getId());
        execution.setProcessDefinitionId(processDefinitionEntity.getId());
        execution.setRootProcessInstanceId(processInstance.getRootProcessInstanceId());
        execution.setActive(true);
        execution.setScope(true);
        execution.setTenantId(processInstance.getTenantId());
        execution.setStartTime(processInstance.getStartTime());
        
        executionEntityManager.insert(execution);

        FlowElement newSubProcess = bpmnModel.getMainProcess().getFlowElement(dynamicEmbeddedSubProcessBuilder.getId(), true);
        execution.setCurrentFlowElement(newSubProcess);

        Context.getAgenda().planContinueProcessOperation(execution);
    }

}