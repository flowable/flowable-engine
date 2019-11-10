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
package org.flowable.cmmn.engine.impl.util;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CmmnLoggingSessionUtil {
    
    public static void addLoggingData(String type, String message, CaseInstanceEntity caseInstanceEntity) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, caseInstanceEntity.getId(), null, ScopeTypes.CMMN);
        loggingNode.put("scopeDefinitionId", caseInstanceEntity.getCaseDefinitionId());
        fillScopeDefinitionInfo(caseInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addLoggingData(type, loggingNode);
    }
    
    public static void addLoggingData(String type, String message, PlanItemInstanceEntity planItemInstanceEntity) {
        addLoggingData(type, message, null, null, planItemInstanceEntity);
    }
    
    public static void addLoggingData(String type, String message, String oldState, String newState, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        String activityId = null;
        String activityName = null;
        String activityType = null;
        String activitySubType = null;
        if (planItemDefinition != null) {
            activityId = planItemDefinition.getId();
            activityName = planItemDefinition.getName();
            activityType = planItemDefinition.getClass().getSimpleName();
            activitySubType = getActivitySubType(planItemDefinition);
        }
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(), 
                        ScopeTypes.CMMN, planItemInstanceEntity.getCaseDefinitionId(), activityId, activityName, activityType, activitySubType);
        loggingNode.put("state", planItemInstanceEntity.getState());
        if (oldState != null) {
            loggingNode.put("oldState", oldState);
        }
        if (newState != null) {
            loggingNode.put("newState", newState);
        }
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addLoggingData(type, loggingNode);
    }
    
    public static void addLoggingData(String type, String message, TaskEntity task, PlanItemInstanceEntity planItemInstanceEntity) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getScopeId(), task.getSubScopeId(), ScopeTypes.CMMN);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("taskId", task.getId());
        putIfNotNull("taskName", task.getName(), loggingNode);
        putIfNotNull("taskCategory", task.getCategory(), loggingNode);
        putIfNotNull("taskFormKey", task.getFormKey(), loggingNode);
        putIfNotNull("taskDescription", task.getDescription(), loggingNode);
        putIfNotNull("taskDueDate", task.getDueDate(), loggingNode);
        putIfNotNull("taskPriority", task.getPriority(), loggingNode);
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        fillPlanItemDefinitionInfo(loggingNode, planItemInstanceEntity);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode);
    }
    
    public static void addExecuteActivityBehaviorLoggingData(String type, PlanItemActivityBehavior activityBehavior, 
                    PlanItemDefinition planItemDefinition, PlanItemInstanceEntity planItemInstanceEntity) {
        
        String message = "In " + planItemDefinition.getClass().getSimpleName() + ", executing " + activityBehavior.getClass().getSimpleName();
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(), ScopeTypes.CMMN);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("elementId", planItemDefinition.getId());
        putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
        loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(planItemDefinition), loggingNode);
        loggingNode.put("activityBehavior", activityBehavior.getClass().getSimpleName());
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode);
    }
    
    public static void addAsyncActivityLoggingData(String message, String type, JobEntity jobEntity, PlanItemDefinition planItemDefinition, PlanItemInstanceEntity planItemInstanceEntity) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(), ScopeTypes.CMMN);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("elementId", planItemDefinition.getId());
        putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
        loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(planItemDefinition), loggingNode);
        loggingNode.put("jobId", jobEntity.getId());
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode);
    }
    
    public static ObjectNode fillBasicTaskLoggingData(String message, TaskEntity task, PlanItemInstanceEntity planItemInstanceEntity) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getScopeId(), task.getSubScopeId(), ScopeTypes.CMMN);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("taskId", task.getId());
        putIfNotNull("taskName", task.getName(), loggingNode);
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        fillPlanItemDefinitionInfo(loggingNode, planItemInstanceEntity);
        
        return loggingNode;
    }
    
    public static void addErrorLoggingData(String type, String message, Throwable t, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        String activityId = null;
        String activityName = null;
        String activityType = null;
        String activitySubType = null;
        if (planItemDefinition != null) {
            activityId = planItemDefinition.getId();
            activityName = planItemDefinition.getName();
            activityType = planItemDefinition.getClass().getSimpleName();
            activitySubType = getActivitySubType(planItemDefinition);
        }
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(), 
                        ScopeTypes.CMMN, planItemInstanceEntity.getCaseDefinitionId(), activityId, activityName, activityType, activitySubType);
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addErrorLoggingData(type, loggingNode, t);
    }
    
    public static void fillLoggingData(ObjectNode loggingNode, PlanItemInstanceEntity planItemInstanceEntitys) {
        loggingNode.put("scopeDefinitionId", planItemInstanceEntitys.getCaseDefinitionId());
        
        fillScopeDefinitionInfo(planItemInstanceEntitys.getCaseDefinitionId(), loggingNode);
        
        PlanItemDefinition planItemDefinition = planItemInstanceEntitys.getPlanItemDefinition();
        if (planItemDefinition != null) {
            loggingNode.put("elementId", planItemDefinition.getId());
            putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
            loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        }
    }
    
    public static void addTaskIdentityLinkData(String type, String message, boolean isUser, List<IdentityLinkEntity> identityLinkEntities, 
                    TaskEntity task, PlanItemInstanceEntity planItemInstanceEntity) {
        
        ObjectNode loggingNode = fillBasicTaskLoggingData(message, task, planItemInstanceEntity);
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
        
        LoggingSessionUtil.addLoggingData(type, loggingNode);
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
    
    protected static String getActivitySubType(PlanItemDefinition planItemDefinition) {
        String activitySubType = null;
        if (planItemDefinition instanceof ServiceTask) {
            ServiceTask serviceTask = (ServiceTask) planItemDefinition;
            activitySubType = serviceTask.getImplementation();
        }
        
        return activitySubType;
    }
    
    protected static void fillScopeDefinitionInfo(String caseDefinitionId, ObjectNode loggingNode) {
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
        loggingNode.put("scopeDefinitionKey", caseDefinition.getKey());
        loggingNode.put("scopeDefinitionName", caseDefinition.getName());
    }
    
    protected static void fillPlanItemDefinitionInfo(ObjectNode loggingNode, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        
        if (planItemDefinition != null) {
            loggingNode.put("elementId", planItemDefinition.getId());
            putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
            loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        }
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
