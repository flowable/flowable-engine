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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EntityLinkUtil;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskBeforeContext;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * @author Joram Barrez
 */
public class HumanTaskActivityBehavior extends TaskActivityBehavior implements PlanItemActivityBehavior {

    protected HumanTask humanTask;

    public HumanTaskActivityBehavior(HumanTask humanTask) {
        super(humanTask.isBlocking(), humanTask.getBlockingExpression());
        this.humanTask = humanTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (evaluateIsBlocking(planItemInstanceEntity)) {

            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            ExpressionManager expressionManager = CommandContextUtil.getExpressionManager(commandContext);

            TaskEntity taskEntity = taskService.createTask();

            taskEntity.setScopeId(planItemInstanceEntity.getCaseInstanceId());
            taskEntity.setSubScopeId(planItemInstanceEntity.getId());
            taskEntity.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            taskEntity.setScopeType(ScopeTypes.CMMN);
            taskEntity.setTenantId(planItemInstanceEntity.getTenantId());

            taskEntity.setTaskDefinitionKey(humanTask.getId());
            
            CreateHumanTaskBeforeContext beforeContext = new CreateHumanTaskBeforeContext(humanTask, planItemInstanceEntity, humanTask.getName(), 
                            humanTask.getDocumentation(), humanTask.getDueDate(), humanTask.getPriority(), humanTask.getCategory(), 
                            humanTask.getFormKey(), humanTask.getAssignee(), humanTask.getOwner(), 
                            humanTask.getCandidateUsers(), humanTask.getCandidateGroups());
            
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (cmmnEngineConfiguration.getCreateHumanTaskInterceptor() != null) {
                cmmnEngineConfiguration.getCreateHumanTaskInterceptor().beforeCreateHumanTask(beforeContext);
            }
            
            handleTaskName(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleTaskDescription(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleAssignee(planItemInstanceEntity, taskService, expressionManager, taskEntity, beforeContext);
            handleOwner(planItemInstanceEntity, taskService, expressionManager, taskEntity, beforeContext);
            handlePriority(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleFormKey(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleDueDate(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleCategory(planItemInstanceEntity, expressionManager, taskEntity, beforeContext);

            TaskHelper.insertTask(taskEntity, true);

            handleCandidateUsers(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);
            handleCandidateGroups(commandContext, planItemInstanceEntity, expressionManager, taskEntity, beforeContext);

            CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskCreated(taskEntity);
            
            if (cmmnEngineConfiguration.isEnableEntityLinks()) {
                EntityLinkUtil.copyExistingEntityLinks(planItemInstanceEntity.getCaseInstanceId(), taskEntity.getId(), ScopeTypes.TASK);
                EntityLinkUtil.createNewEntityLink(planItemInstanceEntity.getCaseInstanceId(), taskEntity.getId(), ScopeTypes.TASK);
            }

            cmmnEngineConfiguration.getListenerNotificationHelper()
                .executeTaskListeners(humanTask, taskEntity, TaskListener.EVENTNAME_CREATE);
            
            if (cmmnEngineConfiguration.getCreateHumanTaskInterceptor() != null) {
                CreateHumanTaskAfterContext afterContext = new CreateHumanTaskAfterContext(humanTask, taskEntity, planItemInstanceEntity);
                cmmnEngineConfiguration.getCreateHumanTaskInterceptor().afterCreateHumanTask(afterContext);
            }

        } else {
            // if not blocking, treat as a manual task. No need to create a task entry.
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation((PlanItemInstanceEntity) planItemInstanceEntity);

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
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        if (StringUtils.isNotEmpty(beforeContext.getAssignee())) {
            Object assigneeExpressionValue = expressionManager.createExpression(beforeContext.getAssignee()).getValue(planItemInstanceEntity);
            String assigneeValue = null;
            if (assigneeExpressionValue != null) {
                assigneeValue = assigneeExpressionValue.toString();
            }

            taskService.changeTaskAssignee(taskEntity, assigneeValue);
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
                } else if (dueDate instanceof String) {

                    String dueDateString = (String) dueDate;
                    if (dueDateString.startsWith("P")) {
                        taskEntity.setDueDate(new DateTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime())
                                .plus(Period.parse(dueDateString)).toDate());
                    } else {
                        taskEntity.setDueDate(DateTime.parse(dueDateString).toDate());
                    }

                } else {
                    throw new FlowableIllegalArgumentException("Due date expression does not resolve to a Date or Date string: " + beforeContext.getDueDate());
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

    protected void handleCandidateUsers(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        List<String> candidateUsers = beforeContext.getCandidateUsers();
        if (candidateUsers != null && !candidateUsers.isEmpty()) {
            for (String candidateUser : candidateUsers) {
                Expression userIdExpr = expressionManager.createExpression(candidateUser);
                Object value = userIdExpr.getValue(planItemInstanceEntity);
                if (value instanceof String) {
                    List<String> candidates = extractCandidates((String) value);
                    taskEntity.addCandidateUsers(candidates);

                } else if (value instanceof Collection) {
                    taskEntity.addCandidateUsers((Collection<String>) value);

                } else {
                    throw new FlowableException("Expression did not resolve to a string or collection of strings");
                }
            }
        }
    }

    protected void handleCandidateGroups(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity, CreateHumanTaskBeforeContext beforeContext) {
        
        List<String> candidateGroups = beforeContext.getCandidateGroups();
        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            for (String candidateGroup : candidateGroups) {
                Expression groupIdExpr = expressionManager.createExpression(candidateGroup);
                Object value = groupIdExpr.getValue(planItemInstanceEntity);
                if (value instanceof String) {
                    List<String> candidates = extractCandidates((String) value);
                    taskEntity.addCandidateGroups(candidates);

                } else if (value instanceof Collection) {
                    taskEntity.addCandidateGroups((Collection<String>) value);

                } else {
                    throw new FlowableIllegalArgumentException("Expression did not resolve to a string or collection of strings");
                }
            }
        }
    }

    protected List<String> extractCandidates(String str) {
        return Arrays.asList(str.split("[\\s]*,[\\s]*"));
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstance) {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
            throw new FlowableException("Can only trigger a human task plan item that is in the ACTIVE state");
        }

        TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
        if (taskEntities == null || taskEntities.isEmpty()) {
            throw new FlowableException("No task entity found for plan item instance " + planItemInstance.getId());
        }

        // Should be only one
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isDeleted()) {
                TaskHelper.deleteTask(taskEntity, null, false, true);
            }
        }

        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation((PlanItemInstanceEntity) planItemInstance);
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
            for (TaskEntity taskEntity : taskEntities) {
                TaskHelper.deleteTask(taskEntity, "cmmn-state-transition-" + transition, false, true);
            }
        }
    }

}
