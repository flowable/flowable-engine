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
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.EntityWithSentryPartInstances;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CmmnLoggingSessionUtil {
    
    public static void addLoggingData(String type, String message, CaseInstanceEntity caseInstanceEntity, ObjectMapper objectMapper) {
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, caseInstanceEntity.getId(), null, ScopeTypes.CMMN, objectMapper);
        loggingNode.put("scopeDefinitionId", caseInstanceEntity.getCaseDefinitionId());
        fillScopeDefinitionInfo(caseInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addLoggingData(String type, String message, PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        addLoggingData(type, message, null, null, planItemInstanceEntity, objectMapper);
    }
    
    public static void addLoggingData(String type, String message, String oldState, String newState, 
            PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillPlanItemInstanceInfo(message, planItemInstanceEntity, objectMapper);
        loggingNode.put("state", planItemInstanceEntity.getState());
        if (oldState != null) {
            loggingNode.put("oldState", oldState);
        }
        if (newState != null) {
            loggingNode.put("newState", newState);
        }
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addLoggingData(String type, String message, TaskEntity task, 
            PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getScopeId(), task.getSubScopeId(), ScopeTypes.CMMN, objectMapper);
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
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addExecuteActivityBehaviorLoggingData(String type, PlanItemActivityBehavior activityBehavior, 
                    PlanItemDefinition planItemDefinition, PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        String message = "In " + planItemDefinition.getClass().getSimpleName() + ", executing " + activityBehavior.getClass().getSimpleName();
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), 
                planItemInstanceEntity.getId(), ScopeTypes.CMMN, objectMapper);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("elementId", planItemDefinition.getId());
        putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
        loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(planItemDefinition), loggingNode);
        loggingNode.put("activityBehavior", activityBehavior.getClass().getSimpleName());
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addAsyncActivityLoggingData(String message, String type, JobEntity jobEntity, PlanItemDefinition planItemDefinition, 
            PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), 
                planItemInstanceEntity.getId(), ScopeTypes.CMMN, objectMapper);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("elementId", planItemDefinition.getId());
        putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
        loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        putIfNotNull("elementSubType", getActivitySubType(planItemDefinition), loggingNode);
        loggingNode.put("jobId", jobEntity.getId());
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    public static ObjectNode fillBasicTaskLoggingData(String message, TaskEntity task, 
            PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = LoggingSessionUtil.fillLoggingData(message, task.getScopeId(), task.getSubScopeId(), 
                ScopeTypes.CMMN, objectMapper);
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        loggingNode.put("taskId", task.getId());
        putIfNotNull("taskName", task.getName(), loggingNode);
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        fillPlanItemDefinitionInfo(loggingNode, planItemInstanceEntity);
        
        return loggingNode;
    }
    
    public static void addEvaluateSentryLoggingData(List<SentryOnPart> sentryOnParts, SentryIfPart sentryIfPart, 
            EntityWithSentryPartInstances instance, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillEvaluateSentryInstanceEntity(instance, objectMapper);
        ArrayNode onPartArrayNode = loggingNode.putArray("onParts");
        for (SentryOnPart onPart : sentryOnParts) {
            ObjectNode onPartNode = onPartArrayNode.addObject();
            onPartNode.put("id", onPart.getId());
            onPartNode.put("source", onPart.getSourceRef());
            onPartNode.put("elementId", onPart.getSource().getPlanItemDefinition().getId());
            onPartNode.put("standardEvent", onPart.getStandardEvent());
        }
        
        ObjectNode ifPartNode = loggingNode.putObject("ifPart");
        ifPartNode.put("condition", sentryIfPart.getCondition());
        
        LoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addEvaluateSentryLoggingData(List<SentryOnPart> sentryOnParts, EntityWithSentryPartInstances instance, ObjectMapper objectMapper) {
        ObjectNode loggingNode = fillEvaluateSentryInstanceEntity(instance, objectMapper);
        ArrayNode onPartArrayNode = loggingNode.putArray("onParts");
        for (SentryOnPart onPart : sentryOnParts) {
            ObjectNode onPartNode = onPartArrayNode.addObject();
            onPartNode.put("id", onPart.getId());
            onPartNode.put("source", onPart.getSourceRef());
            onPartNode.put("elementId", onPart.getSource().getPlanItemDefinition().getId());
            onPartNode.put("standardEvent", onPart.getStandardEvent());
        }
        
        LoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addEvaluateSentryLoggingData(SentryIfPart sentryIfPart, EntityWithSentryPartInstances instance, ObjectMapper objectMapper) {
        ObjectNode loggingNode = fillEvaluateSentryInstanceEntity(instance, objectMapper);
        ObjectNode ifPartNode = loggingNode.putObject("ifPart");
        ifPartNode.put("condition", sentryIfPart.getCondition());
        
        LoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY, loggingNode, ScopeTypes.CMMN);
    }
    
    public static void addEvaluateSentryFailedLoggingData(SentryIfPart sentryIfPart, RuntimeException e, 
            EntityWithSentryPartInstances instance, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillEvaluateSentryInstanceEntity(instance, objectMapper);
        
        String label = null;
        if (instance instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            label = planItemInstanceEntity.getPlanItemDefinitionId();
            if (StringUtils.isNotEmpty(planItemInstanceEntity.getPlanItemDefinition().getName())) {
                label = planItemInstanceEntity.getPlanItemDefinition().getName();
            }
        } else {
            label = instance.getId();
        }
        
        loggingNode.put("message", "IfPart evaluation failed for " + label);
        ObjectNode ifPartNode = loggingNode.putObject("ifPart");
        ifPartNode.put("condition", sentryIfPart.getCondition());
        LoggingSessionUtil.addErrorLoggingData(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY_FAILED, loggingNode, e, ScopeTypes.CMMN);
    }
    
    public static void addErrorLoggingData(String type, String message, Throwable t, 
            PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillPlanItemInstanceInfo(message, planItemInstanceEntity, objectMapper);
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        LoggingSessionUtil.addErrorLoggingData(type, loggingNode, t, ScopeTypes.CMMN);
    }
    
    public static void fillLoggingData(ObjectNode loggingNode, PlanItemInstanceEntity planItemInstanceEntity) {
        loggingNode.put("scopeDefinitionId", planItemInstanceEntity.getCaseDefinitionId());
        
        fillScopeDefinitionInfo(planItemInstanceEntity.getCaseDefinitionId(), loggingNode);
        
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        if (planItemDefinition != null) {
            loggingNode.put("elementId", planItemDefinition.getId());
            putIfNotNull("elementName", planItemDefinition.getName(), loggingNode);
            loggingNode.put("elementType", planItemDefinition.getClass().getSimpleName());
        }
    }
    
    public static void fillLoggingData(ObjectNode loggingNode, CaseInstanceEntity caseInstanceEntity) {
        loggingNode.put("scopeDefinitionId", caseInstanceEntity.getCaseDefinitionId());
        
        fillScopeDefinitionInfo(caseInstanceEntity.getCaseDefinitionId(), loggingNode);
    }
    
    public static void addTaskIdentityLinkData(String type, String message, boolean isUser, List<IdentityLinkEntity> identityLinkEntities, 
                    TaskEntity task, PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
        
        ObjectNode loggingNode = fillBasicTaskLoggingData(message, task, planItemInstanceEntity, objectMapper);
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
        
        LoggingSessionUtil.addLoggingData(type, loggingNode, ScopeTypes.CMMN);
    }
    
    protected static String getActivitySubType(PlanItemDefinition planItemDefinition) {
        String activitySubType = null;
        if (planItemDefinition instanceof ServiceTask serviceTask) {
            activitySubType = serviceTask.getImplementation();
        }
        
        return activitySubType;
    }
    
    protected static void fillScopeDefinitionInfo(String caseDefinitionId, ObjectNode loggingNode) {
        if (!loggingNode.has("scopeDefinitionId")) {
            loggingNode.put("scopeDefinitionId", caseDefinitionId);
        }
        CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
        loggingNode.put("scopeDefinitionKey", caseDefinition.getKey());
        loggingNode.put("scopeDefinitionName", caseDefinition.getName());
    }
    
    protected static ObjectNode fillPlanItemInstanceInfo(String message, PlanItemInstanceEntity planItemInstanceEntity, ObjectMapper objectMapper) {
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
        
        return LoggingSessionUtil.fillLoggingData(message, planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(), 
                        ScopeTypes.CMMN, planItemInstanceEntity.getCaseDefinitionId(), activityId, 
                        activityName, activityType, activitySubType, objectMapper);
    }
    
    protected static ObjectNode fillEvaluateSentryInstanceEntity(EntityWithSentryPartInstances instance, ObjectMapper objectMapper) {
        ObjectNode loggingNode = null;
        String caseDefinitionId = null;
        if (instance instanceof PlanItemInstanceEntity planItemInstanceEntity) {
            String label = planItemInstanceEntity.getPlanItemDefinitionId();
            if (StringUtils.isNotEmpty(planItemInstanceEntity.getPlanItemDefinition().getName())) {
                label = planItemInstanceEntity.getPlanItemDefinition().getName();
            }
            loggingNode = fillPlanItemInstanceInfo("Evaluate sentry parts for " + label, planItemInstanceEntity, objectMapper);
            caseDefinitionId = planItemInstanceEntity.getCaseDefinitionId();
        
        } else {
            CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) instance;
            loggingNode = LoggingSessionUtil.fillLoggingData("Evaluate sentry parts for case instance " + instance.getId(), 
                            caseInstanceEntity.getId(), null, ScopeTypes.CMMN, objectMapper);
            caseDefinitionId = caseInstanceEntity.getCaseDefinitionId();
        }
        
        fillScopeDefinitionInfo(caseDefinitionId, loggingNode);
        return loggingNode;
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
