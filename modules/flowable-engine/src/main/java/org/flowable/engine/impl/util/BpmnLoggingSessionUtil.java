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
package org.flowable.engine.impl.util;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskDelegateExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskExpressionActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ClassDelegate;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BpmnLoggingSessionUtil {
    
    public static void addLoggingData(String type, String message, DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        String activityId = null;
        String activityName = null;
        String activityType = null;
        String activitySubType = null;
        if (flowElement != null) {
            activityId = flowElement.getId();
            activityName = flowElement.getName();
            activityType = flowElement.getClass().getSimpleName();
            activitySubType = getActivitySubType(flowElement);
        }
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, execution.getProcessInstanceId(), execution.getId(), 
                        ScopeTypes.BPMN, execution.getProcessDefinitionId(), activityId, activityName, activityType, activitySubType, getObjectMapper());
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static void addLoggingData(String type, String message, TaskEntity task, DelegateExecution execution) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getProcessInstanceId(), task.getExecutionId(), ScopeTypes.BPMN, getObjectMapper());
        loggingNode.put("scopeDefinitionId", execution.getProcessDefinitionId());
        loggingNode.put("taskId", task.getId());
        putIfNotNull("taskName", task.getName(), loggingNode);
        putIfNotNull("taskCategory", task.getCategory(), loggingNode);
        putIfNotNull("taskFormKey", task.getFormKey(), loggingNode);
        putIfNotNull("taskDescription", task.getDescription(), loggingNode);
        putIfNotNull("taskDueDate", task.getDueDate(), loggingNode);
        putIfNotNull("taskPriority", task.getPriority(), loggingNode);
        
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        fillFlowElementInfo(loggingNode, execution);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static void addExecuteActivityBehaviorLoggingData(String type, ActivityBehavior activityBehavior, FlowNode flowNode, ExecutionEntity execution) {
        String message = "In " + flowNode.getClass().getSimpleName() + ", executing " + activityBehavior.getClass().getSimpleName();
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, execution.getProcessInstanceId(), execution.getId(), ScopeTypes.BPMN, getObjectMapper());
        loggingNode.put("scopeDefinitionId", execution.getProcessDefinitionId());
        loggingNode.put("elementId", flowNode.getId());
        putIfNotNull("elementName", flowNode.getName(), loggingNode);
        loggingNode.put("elementType", flowNode.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(flowNode), loggingNode);
        loggingNode.put("activityBehavior", activityBehavior.getClass().getSimpleName());
        
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static void addAsyncActivityLoggingData(String message, String type, JobEntity jobEntity, FlowElement flowElement, ExecutionEntity execution) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, execution.getProcessInstanceId(), execution.getId(), ScopeTypes.BPMN, getObjectMapper());
        loggingNode.put("scopeDefinitionId", execution.getProcessDefinitionId());
        loggingNode.put("elementId", flowElement.getId());
        putIfNotNull("elementName", flowElement.getName(), loggingNode);
        loggingNode.put("elementType", flowElement.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(flowElement), loggingNode);
        loggingNode.put("jobId", jobEntity.getId());
        
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static void addSequenceFlowLoggingData(String type, ExecutionEntity execution) {
        String message = null;
        FlowElement flowElement = execution.getCurrentFlowElement();
        SequenceFlow sequenceFlow = null;
        if (flowElement instanceof SequenceFlow) {
            sequenceFlow = (SequenceFlow) flowElement;
            String sequenceFlowId = "";
            if (sequenceFlow.getId() != null) {
                sequenceFlowId = sequenceFlow.getId() + ", ";
            }
            message = "Sequence flow will be taken for " + sequenceFlowId + sequenceFlow.getSourceRef() + " --> " + sequenceFlow.getTargetRef();
        } else {
            message = "Sequence flow will be taken";
        }
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, execution.getProcessInstanceId(), execution.getId(), ScopeTypes.BPMN, getObjectMapper());
        loggingNode.put("scopeDefinitionId", execution.getProcessDefinitionId());
        if (sequenceFlow != null) {
            putIfNotNull("elementId", sequenceFlow.getId(), loggingNode);
            putIfNotNull("elementName", sequenceFlow.getName(), loggingNode);
            loggingNode.put("elementType", sequenceFlow.getClass().getSimpleName());
            putIfNotNull("sourceRef", sequenceFlow.getSourceRef(), loggingNode);
            putIfNotNull("targetRef", sequenceFlow.getTargetRef(), loggingNode);
        }
        
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static ObjectNode fillBasicTaskLoggingData(String message, TaskEntity task, DelegateExecution execution) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getProcessInstanceId(), task.getExecutionId(), ScopeTypes.BPMN, getObjectMapper());
        loggingNode.put("scopeDefinitionId", execution.getProcessDefinitionId());
        loggingNode.put("taskId", task.getId());
        putIfNotNull("taskName", task.getName(), loggingNode);
        
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        fillFlowElementInfo(loggingNode, execution);
        
        return loggingNode;
    }
    
    public static void addErrorLoggingData(String type, String message, Throwable t, DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        String activityId = null;
        String activityName = null;
        String activityType = null;
        String activitySubType = null;
        if (flowElement != null) {
            activityId = flowElement.getId();
            activityName = flowElement.getName();
            activityType = flowElement.getClass().getSimpleName();
            activitySubType = getActivitySubType(flowElement);
        }
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, execution.getProcessInstanceId(), execution.getId(), 
                        ScopeTypes.BPMN, execution.getProcessDefinitionId(), activityId, activityName, activityType, activitySubType, getObjectMapper());
        fillScopeDefinitionInfo(execution.getProcessDefinitionId(), loggingNode);
        LoggingSessionUtil.addErrorLoggingData(type, loggingNode, t, ScopeTypes.BPMN);
    }
    
    public static void fillLoggingData(ObjectNode loggingNode, ExecutionEntity executionEntity) {
        loggingNode.put("scopeDefinitionId", executionEntity.getProcessDefinitionId());
        
        fillScopeDefinitionInfo(executionEntity.getProcessDefinitionId(), loggingNode);
        
        FlowElement flowElement = executionEntity.getCurrentFlowElement();
        if (flowElement == null) {
            flowElement = executionEntity.getOriginatingCurrentFlowElement();
        }
        
        if (flowElement != null) {
            loggingNode.put("elementId", flowElement.getId());
            putIfNotNull("elementName", flowElement.getName(), loggingNode);
            loggingNode.put("elementType", flowElement.getClass().getSimpleName());
        }
    }
    
    public static void addTaskIdentityLinkData(String type, String message, boolean isUser, List<IdentityLinkEntity> identityLinkEntities, 
                    TaskEntity task, DelegateExecution execution) {
        
        ObjectNode loggingNode = fillBasicTaskLoggingData(message, task, execution);
        ArrayNode identityLinkArray = null;
        if (isUser) {
            identityLinkArray = loggingNode.putArray("taskUserIdentityLinks");
        } else {
            identityLinkArray = loggingNode.putArray("taskGroupIdentityLinks");
        }
        
        for (IdentityLinkEntity identityLink : identityLinkEntities) {
            ObjectNode identityLinkNode = identityLinkArray.addObject();
            identityLinkNode.put("id", identityLink.getId());
            identityLinkNode.put("type", identityLink.getType());
            if (isUser) {
                identityLinkNode.put("userId", identityLink.getUserId());
            } else {
                identityLinkNode.put("groupId", identityLink.getGroupId());
            }
        }
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.BPMN);
    }
    
    public static String getBoundaryCreateEventType(BoundaryEvent boundaryEvent) {
        List<EventDefinition> eventDefinitions = boundaryEvent.getEventDefinitions();
        if (eventDefinitions != null && !eventDefinitions.isEmpty()) {
            EventDefinition eventDefinition = eventDefinitions.get(0);
            if (eventDefinition instanceof TimerEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_TIMER_EVENT_CREATE;
            } else if (eventDefinition instanceof MessageEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_MESSAGE_EVENT_CREATE;
            } else if (eventDefinition instanceof SignalEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_SIGNAL_EVENT_CREATE;
            } else if (eventDefinition instanceof CancelEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_CANCEL_EVENT_CREATE;
            } else if (eventDefinition instanceof CompensateEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_COMPENSATE_EVENT_CREATE;
            } else if (eventDefinition instanceof ConditionalEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_CONDITIONAL_EVENT_CREATE;
            } else if (eventDefinition instanceof EscalationEventDefinition) {
                return LoggingSessionConstants.TYPE_BOUNDARY_ESCALATION_EVENT_CREATE;
            }
        }
        
        return LoggingSessionConstants.TYPE_BOUNDARY_EVENT_CREATE;
    }
    
    public static String getBoundaryEventType(BoundaryEvent boundaryEvent) {
        List<EventDefinition> eventDefinitions = boundaryEvent.getEventDefinitions();
        if (eventDefinitions != null && !eventDefinitions.isEmpty()) {
            EventDefinition eventDefinition = eventDefinitions.get(0);
            return eventDefinition.getClass().getSimpleName();
        }
        
        return "unknown";
    }
    
    protected static String getActivitySubType(FlowElement flowElement) {
        String activitySubType = null;
        if (flowElement instanceof Event event) {
            List<EventDefinition> eventDefinitions = event.getEventDefinitions();
            if (eventDefinitions != null && !eventDefinitions.isEmpty()) {
                EventDefinition eventDefinition = eventDefinitions.get(0);
                activitySubType = eventDefinition.getClass().getSimpleName();
            }
            
        } else if (flowElement instanceof ServiceTask serviceTask) {
            if (serviceTask.getBehavior() != null && serviceTask.getBehavior() instanceof ClassDelegate classDelegate) {
                activitySubType = classDelegate.getClassName();
                
            } else if (serviceTask.getBehavior() != null && serviceTask.getBehavior() instanceof ServiceTaskExpressionActivityBehavior) {
                activitySubType = serviceTask.getImplementation();
            
            } else if (serviceTask.getBehavior() != null && serviceTask.getBehavior() instanceof ServiceTaskDelegateExpressionActivityBehavior) {
                activitySubType = serviceTask.getImplementation();
            }
        }
        
        return activitySubType;
    }
    
    protected static void fillScopeDefinitionInfo(String processDefinitionId, ObjectNode loggingNode) {
        if (!loggingNode.has("scopeDefinitionId")) {
            loggingNode.put("scopeDefinitionId", processDefinitionId);
        }
        ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
        loggingNode.put("scopeDefinitionKey", processDefinition.getKey());
        loggingNode.put("scopeDefinitionName", processDefinition.getName());
    }
    
    protected static void fillFlowElementInfo(ObjectNode loggingNode, DelegateExecution execution) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        
        if (flowElement != null) {
            loggingNode.put("elementId", flowElement.getId());
            putIfNotNull("elementName", flowElement.getName(), loggingNode);
            loggingNode.put("elementType", flowElement.getClass().getSimpleName());
        }
    }
    
    protected static ObjectMapper getObjectMapper() {
        return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();
    }
    
    protected static void putIfNotNull(String name, String value, ObjectNode loggingNode) {
        if (StringUtils.isNotEmpty(value)) {
            loggingNode.put(name, value);
        }
    }
    
    protected static void putIfNotNull(String name, Integer value, ObjectNode loggingNode) {
        if (value != null) {
            loggingNode.put(name, value);
        }
    }
    
    protected static void putIfNotNull(String name, Date value, ObjectNode loggingNode) {
        if (value != null) {
            loggingNode.put(name, LoggingSessionUtil.formatDate(value));
        }
    }
}
