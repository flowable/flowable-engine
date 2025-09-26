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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityWithMigrationContextBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.event.FlowableCmmnEventBuilder;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskBeforeContext;
import org.flowable.cmmn.engine.interceptor.MigrationContext;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.assignment.CandidateUtil;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class HumanTaskActivityBehavior extends TaskActivityBehavior implements PlanItemActivityBehavior, CmmnActivityWithMigrationContextBehavior {

    protected HumanTask humanTask;

    public HumanTaskActivityBehavior(HumanTask humanTask) {
        super(humanTask.isBlocking(), humanTask.getBlockingExpression());
        this.humanTask = humanTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        execute(commandContext, planItemInstanceEntity, null);
    }
    
    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, MigrationContext migrationContext) {
        if (evaluateIsBlocking(planItemInstanceEntity)) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
            ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);
            PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);

            TaskEntity taskEntity = taskService.createTask();

            taskEntity.setScopeId(planItemInstanceEntity.getCaseInstanceId());
            taskEntity.setSubScopeId(planItemInstanceEntity.getId());
            taskEntity.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            taskEntity.setScopeType(ScopeTypes.CMMN);
            taskEntity.setTenantId(planItemInstanceEntity.getTenantId());

            // set the stage instance id, if this plan item (task) belongs to a stage
            taskEntity.setPropagatedStageInstanceId(planItemInstanceEntity.getStageInstanceId());

            taskEntity.setTaskDefinitionKey(humanTask.getId());

            String taskName = humanTask.getName();
            if (planItemInstanceEntity.getName() != null) {
                taskName = planItemInstanceEntity.getName();
            }
            
            CreateHumanTaskBeforeContext beforeContext = new CreateHumanTaskBeforeContext(humanTask, planItemInstanceEntity, taskName,
                    humanTask.getDocumentation(), humanTask.getDueDate(), humanTask.getPriority(), humanTask.getCategory(), 
                    humanTask.getFormKey(), humanTask.getAssignee(), humanTask.getOwner(), 
                    humanTask.getCandidateUsers(), humanTask.getCandidateGroups());
            
            if (cmmnEngineConfiguration.getCreateHumanTaskInterceptor() != null) {
                cmmnEngineConfiguration.getCreateHumanTaskInterceptor().beforeCreateHumanTask(beforeContext);
            }
            
            handleTaskName(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleTaskDescription(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleAssignee(planItemInstanceEntity, taskService, expressionManager, taskEntity, planItemInstanceEntityManager, beforeContext, migrationContext);
            handleOwner(planItemInstanceEntity, taskService, expressionManager, taskEntity, beforeContext);
            handlePriority(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleFormKey(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleDueDate(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleCategory(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);

            TaskHelper.insertTask(taskEntity, true, cmmnEngineConfiguration);
            
            if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                CmmnLoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_CREATE, "Human task '" + 
                                taskEntity.getName() + "' created", taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
                
                if (StringUtils.isNotEmpty(taskEntity.getAssignee())) {
                    ObjectNode loggingNode = CmmnLoggingSessionUtil.fillBasicTaskLoggingData("Set task assignee value to " + 
                            taskEntity.getAssignee(), taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
                    loggingNode.put("taskAssignee", taskEntity.getAssignee());
                    LoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_ASSIGNEE, loggingNode, ScopeTypes.CMMN);
                }
                
                if (StringUtils.isNotEmpty(taskEntity.getOwner())) {
                    ObjectNode loggingNode = CmmnLoggingSessionUtil.fillBasicTaskLoggingData("Set task owner value to " + 
                taskEntity.getOwner(), taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
                    loggingNode.put("taskOwner", taskEntity.getOwner());
                    LoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_OWNER, loggingNode, ScopeTypes.CMMN);
                }
            }

            handleCandidateUsers(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleCandidateGroups(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleTaskIdVariableStorage(planItemInstanceEntity, humanTask, expressionManager, taskEntity);

            planItemInstanceEntity.setReferenceId(taskEntity.getId());
            planItemInstanceEntity.setReferenceType(ReferenceTypes.PLAN_ITEM_CHILD_HUMAN_TASK);

            if (cmmnEngineConfiguration.isEnableEntityLinks()) {
                EntityLinkUtil.createEntityLinks(planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId(),
                        planItemInstanceEntity.getPlanItemDefinitionId(), taskEntity.getId(), ScopeTypes.TASK, cmmnEngineConfiguration);
            }

            if (cmmnEngineConfiguration.getCreateHumanTaskInterceptor() != null) {
                CreateHumanTaskAfterContext afterContext = new CreateHumanTaskAfterContext(humanTask, taskEntity, planItemInstanceEntity);
                cmmnEngineConfiguration.getCreateHumanTaskInterceptor().afterCreateHumanTask(afterContext);
            }

            CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskCreated(taskEntity);

            cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(
                    humanTask, taskEntity, TaskListener.EVENTNAME_CREATE);

            FlowableEventDispatcher eventDispatcher = cmmnEngineConfiguration.getTaskServiceConfiguration().getEventDispatcher();
            if (eventDispatcher != null  && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableCmmnEventBuilder.createTaskCreatedEvent(taskEntity), cmmnEngineConfiguration.getEngineCfgKey());
            }

        } else {
            // if not blocking, treat as a manual task. No need to create a task entry.
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);

        }
    }

    protected void handleTaskName(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager, 
                    TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getName())) {
            Object name = expressionManager.createExpression(beforeContext.getName()).getValue(planItemInstanceEntity);
            if (name != null) {
                if (name instanceof String) {
                    taskEntity.setName((String) name);
                } else {
                    throw new FlowableIllegalArgumentException("name expression does not resolve to a string: " + beforeContext.getName());
                }
            }
        }
    }

    protected void handleTaskDescription(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager, 
                    TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getDescription())) {
            Object description = expressionManager.createExpression(beforeContext.getDescription()).getValue(planItemInstanceEntity);
            if (description != null) {
                if (description instanceof String) {
                    taskEntity.setDescription((String) description);
                } else {
                    throw new FlowableIllegalArgumentException("documentation expression does not resolve to a string: " + beforeContext.getDescription());
                }
            }
        }
    }

    protected void handleAssignee(PlanItemInstanceEntity planItemInstanceEntity, TaskService taskService,
            ExpressionManager expressionManager, TaskEntity taskEntity, PlanItemInstanceEntityManager planItemInstanceEntityManager,
            CreateHumanTaskBeforeContext beforeContext, MigrationContext migrationContext) {
        
        String assigneeStringValue = null;
        if (migrationContext != null && migrationContext.getAssignee() != null) {
            assigneeStringValue = migrationContext.getAssignee();
            
        } else if (StringUtils.isNotEmpty(beforeContext.getAssignee())) {
            assigneeStringValue = beforeContext.getAssignee();
        }
        
        if (StringUtils.isNotEmpty(assigneeStringValue)) {
            Object assigneeExpressionValue = expressionManager.createExpression(assigneeStringValue).getValue(planItemInstanceEntity);
            String assigneeValue = null;
            if (assigneeExpressionValue != null) {
                assigneeValue = assigneeExpressionValue.toString();
            }

            taskService.changeTaskAssignee(taskEntity, assigneeValue);
            planItemInstanceEntityManager.updateHumanTaskPlanItemInstanceAssignee(taskEntity, assigneeValue);
        }
    }

    protected void handleOwner(PlanItemInstanceEntity planItemInstanceEntity, TaskService taskService,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getOwner())) {
            Object ownerExpressionValue = expressionManager.createExpression(beforeContext.getOwner()).getValue(planItemInstanceEntity);
            String ownerValue = null;
            if (ownerExpressionValue != null) {
                ownerValue = ownerExpressionValue.toString();
            }

            taskService.changeTaskOwner(taskEntity, ownerValue);
        }
    }

    protected void handlePriority(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager, 
                    TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getPriority())) {
            Object priority = expressionManager.createExpression(beforeContext.getPriority()).getValue(planItemInstanceEntity);
            if (priority != null) {
                if (priority instanceof String) {
                    try {
                        taskEntity.setPriority(Integer.valueOf((String) priority));
                    } catch (NumberFormatException e) {
                        throw new FlowableIllegalArgumentException("Priority does not resolve to a number: " + beforeContext.getPriority(), e);
                    }
                } else if (priority instanceof Number) {
                    taskEntity.setPriority(((Number) priority).intValue());
                } else {
                    throw new FlowableIllegalArgumentException("Priority expression does not resolve to a number: " + beforeContext.getPriority());
                }
            }
        }
    }

    protected void handleFormKey(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager,
            TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {

        if (StringUtils.isNotEmpty(beforeContext.getFormKey())) {
            Object formKey = expressionManager.createExpression(beforeContext.getFormKey()).getValue(planItemInstanceEntity);
            if (formKey != null) {
                if (formKey instanceof String) {
                    taskEntity.setFormKey((String) formKey);
                } else {
                    throw new FlowableIllegalArgumentException("FormKey expression does not resolve to a string: " + beforeContext.getFormKey());
                }
            }
        }
    }

    protected void handleDueDate(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getDueDate())) {
            Object dueDate = expressionManager.createExpression(beforeContext.getDueDate()).getValue(planItemInstanceEntity);
            if (dueDate != null) {
                if (dueDate instanceof Date) {
                    taskEntity.setDueDate((Date) dueDate);

                } else if (dueDate instanceof String dueDateString) {
                    Date resolvedDuedate = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getBusinessCalendarManager()
                            .getBusinessCalendar(DueDateBusinessCalendar.NAME)
                            .resolveDuedate(dueDateString);
                    taskEntity.setDueDate(resolvedDuedate);

                } else if (dueDate instanceof Instant) {
                    taskEntity.setDueDate(Date.from((Instant) dueDate));

                } else if (dueDate instanceof LocalDate) {
                    taskEntity.setDueDate(Date.from(((LocalDate) dueDate).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));

                }  else if (dueDate instanceof LocalDateTime) {
                    taskEntity.setDueDate(Date.from(((LocalDateTime) dueDate).atZone(ZoneId.systemDefault()).toInstant()));

                } else {
                    throw new FlowableIllegalArgumentException("Due date expression does not resolve to a Date, Instant, LocalDate, LocalDateTime or Date string: " + beforeContext.getDueDate());
                }
            }
        }
    }

    protected void handleCategory(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager,
            TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getCategory())) {
            final Object category = expressionManager.createExpression(beforeContext.getCategory()).getValue(planItemInstanceEntity);
            if (category != null) {
                if (category instanceof String) {
                    taskEntity.setCategory((String) category);
                } else {
                    throw new FlowableIllegalArgumentException("Category expression does not resolve to a string: " + beforeContext.getCategory());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleCandidateUsers(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        List<String> candidateUsers = beforeContext.getCandidateUsers();
        if (candidateUsers != null && !candidateUsers.isEmpty()) {
            List<IdentityLinkEntity> allIdentityLinkEntities = new ArrayList<>();
            for (String candidateUser : candidateUsers) {
                Expression userIdExpr = expressionManager.createExpression(candidateUser);
                Object value = userIdExpr.getValue(planItemInstanceEntity);
                Collection<String> candidates = CandidateUtil.extractCandidates(value);
                List<IdentityLinkEntity> identityLinkEntities = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                        .getIdentityLinkService().addCandidateUsers(taskEntity.getId(), candidates);

                if (identityLinkEntities != null && !identityLinkEntities.isEmpty()) {
                    IdentityLinkUtil.handleTaskIdentityLinkAdditions(taskEntity, identityLinkEntities, cmmnEngineConfiguration);
                    allIdentityLinkEntities.addAll(identityLinkEntities);
                }
            }
            
            if (!allIdentityLinkEntities.isEmpty()) {
                if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                    CmmnLoggingSessionUtil.addTaskIdentityLinkData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_USER_IDENTITY_LINKS, 
                            "Added " + allIdentityLinkEntities.size() + " candidate user identity links to task", true,
                            allIdentityLinkEntities, taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleCandidateGroups(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        List<String> candidateGroups = beforeContext.getCandidateGroups();
        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            List<IdentityLinkEntity> allIdentityLinkEntities = new ArrayList<>();
            for (String candidateGroup : candidateGroups) {
                Expression groupIdExpr = expressionManager.createExpression(candidateGroup);
                Object value = groupIdExpr.getValue(planItemInstanceEntity);
                Collection<String> candidates = CandidateUtil.extractCandidates(value);
                List<IdentityLinkEntity> identityLinkEntities = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                        .getIdentityLinkService().addCandidateGroups(taskEntity.getId(), candidates);

                if (identityLinkEntities != null && !identityLinkEntities.isEmpty()) {
                    IdentityLinkUtil.handleTaskIdentityLinkAdditions(taskEntity, identityLinkEntities, cmmnEngineConfiguration);
                    allIdentityLinkEntities.addAll(identityLinkEntities);
                }
            }
            
            if (!allIdentityLinkEntities.isEmpty()) {
                if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                    CmmnLoggingSessionUtil.addTaskIdentityLinkData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_USER_IDENTITY_LINKS, 
                            "Added " + allIdentityLinkEntities.size() + " candidate group identity links to task", true,
                            allIdentityLinkEntities, taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
                }
            }
        }
    }

    private void handleTaskIdVariableStorage(PlanItemInstanceEntity planItemInstanceEntity, HumanTask humanTask, ExpressionManager expressionManager, TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getTaskIdVariableName())) {
            Expression expression = expressionManager.createExpression(humanTask.getTaskIdVariableName());
            String idVariableName = (String) expression.getValue(planItemInstanceEntity);
            if (StringUtils.isNotEmpty(idVariableName)) {
                planItemInstanceEntity.setVariable(idVariableName, taskEntity.getId());
            }
        }
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableIllegalStateException("Can only trigger a human task plan item that is in the ACTIVE state");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
        List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
        if (taskEntities == null || taskEntities.isEmpty()) {
            throw new FlowableException("No task entity found for " + planItemInstance);
        }

        // Should be only one
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isDeleted()) {
                TaskHelper.completeTask(taskEntity, taskEntity.getTempCompletedBy(), cmmnEngineConfiguration);
            }
        }

        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
            List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
            for (TaskEntity taskEntity : taskEntities) {
                TaskHelper.deleteTask(taskEntity, "cmmn-state-transition-" + transition, false, true, cmmnEngineConfiguration);
            }
        } else if (PlanItemTransition.COMPLETE.equals(transition)) {
            if (humanTask.getTaskCompleterVariableName() != null) {

                ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);
                Expression expression = expressionManager.createExpression(humanTask.getTaskCompleterVariableName());
                String completerVariableName = (String) expression.getValue(planItemInstance);
                String completer = Authentication.getAuthenticatedUserId();

                planItemInstance.setVariable(completerVariableName, completer);
            }
        }
    }

}
