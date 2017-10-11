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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.type.VariableScopeType;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * @author Joram Barrez
 */
public class HumanTaskActivityBehavior extends TaskActivityBehavior {
    
    protected HumanTask humanTask;
    
    public HumanTaskActivityBehavior(HumanTask humanTask) {
        super(humanTask);
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
            taskEntity.setScopeType(VariableScopeType.CMMN);
            
            taskEntity.setTaskDefinitionKey(humanTask.getId());
            
            handleAssignee(planItemInstanceEntity, taskService, expressionManager, taskEntity);
            handleOwner(planItemInstanceEntity, taskService, expressionManager, taskEntity);
            handlePrioroty(planItemInstanceEntity, expressionManager, taskEntity);
            handleFormKey(planItemInstanceEntity, expressionManager, taskEntity);
            handleDueDate(commandContext, planItemInstanceEntity, expressionManager, taskEntity);
            handleCategory(planItemInstanceEntity, expressionManager, taskEntity);
            handleCandidateUsers(commandContext, planItemInstanceEntity, expressionManager, taskEntity);
            handleCandidateGroups(commandContext, planItemInstanceEntity, expressionManager, taskEntity);
            
            taskService.insertTask(taskEntity, true);
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskCreated(taskEntity);
            
        } else {
            // if not blocking, treat as a manual task. No need to create a task entry.
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstance((PlanItemInstanceEntity) planItemInstanceEntity);
            
        }
    }

    protected void handleAssignee(PlanItemInstanceEntity planItemInstanceEntity, TaskService taskService,
            ExpressionManager expressionManager, TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getAssignee())) {
            Object assigneeExpressionValue = expressionManager.createExpression(humanTask.getAssignee()).getValue(planItemInstanceEntity);
            String assigneeValue = null;
            if (assigneeExpressionValue != null) {
                assigneeValue = assigneeExpressionValue.toString();
            }

            taskService.changeTaskAssignee(taskEntity, assigneeValue);
        }
    }

    protected void handleOwner(PlanItemInstanceEntity planItemInstanceEntity, TaskService taskService,
            ExpressionManager expressionManager, TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getOwner())) {
            Object ownerExpressionValue = expressionManager.createExpression(humanTask.getOwner()).getValue(planItemInstanceEntity);
            String ownerValue = null;
            if (ownerExpressionValue != null) {
                ownerValue = ownerExpressionValue.toString();
            }

            taskService.changeTaskOwner(taskEntity, ownerValue);
        }
    }

    protected void handlePrioroty(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager,
            TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getPriority())) {
            Object priority = expressionManager.createExpression(humanTask.getPriority()).getValue(planItemInstanceEntity);
            if (priority != null) {
                if (priority instanceof String) {
                    try {
                        taskEntity.setPriority(Integer.valueOf((String) priority));
                    } catch (NumberFormatException e) {
                        throw new FlowableIllegalArgumentException("Priority does not resolve to a number: " + humanTask.getPriority(), e);
                    }
                } else if (priority instanceof Number) {
                    taskEntity.setPriority(((Number) priority).intValue());
                } else {
                    throw new FlowableIllegalArgumentException("Priority expression does not resolve to a number: " + humanTask.getPriority());
                }
            }
        }
    }

    protected void handleFormKey(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager,
            TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getFormKey())) {
            Object formKey = expressionManager.createExpression(humanTask.getFormKey()).getValue(planItemInstanceEntity);
            if (formKey != null) {
                if (formKey instanceof String) {
                    taskEntity.setFormKey((String) formKey);
                } else {
                    throw new FlowableIllegalArgumentException("FormKey expression does not resolve to a string: " + humanTask.getFormKey());
                }
            }
        }
    }

    protected void handleDueDate(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getDueDate())) {
            Object dueDate = expressionManager.createExpression(humanTask.getDueDate()).getValue(planItemInstanceEntity);
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
                    throw new FlowableIllegalArgumentException("Due date expression does not resolve to a Date or Date string: " + humanTask.getDueDate());
                }
            }
        }
    }

    protected void handleCategory(PlanItemInstanceEntity planItemInstanceEntity, ExpressionManager expressionManager,
            TaskEntity taskEntity) {
        if (StringUtils.isNotEmpty(humanTask.getCategory())) {
            final Object category = expressionManager.createExpression(humanTask.getCategory()).getValue(planItemInstanceEntity);
            if (category != null) {
                if (category instanceof String) {
                    taskEntity.setCategory((String) category);
                } else {
                    throw new FlowableIllegalArgumentException("Category expression does not resolve to a string: " + humanTask.getCategory());
                }
            }
        }
    }

    protected void handleCandidateUsers(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity) {
        List<String> candidateUsers = humanTask.getCandidateUsers();
        if (candidateUsers != null && !candidateUsers.isEmpty()) {
            for (String candidateUser : candidateUsers) {
                Expression userIdExpr = expressionManager.createExpression(candidateUser);
                Object value = userIdExpr.getValue(planItemInstanceEntity);
                if (value instanceof String) {
                    List<String> candidates = extractCandidates((String) value);
                    handleIdentityLinks(commandContext, taskEntity, CommandContextUtil.getIdentityLinkService().addCandidateUsers(taskEntity.getId(), candidates));
                    
                } else if (value instanceof Collection) {
                    handleIdentityLinks(commandContext, taskEntity, CommandContextUtil.getIdentityLinkService().addCandidateUsers(taskEntity.getId(), (Collection) value));
                    
                } else {
                    throw new FlowableException("Expression did not resolve to a string or collection of strings");
                }
            }
        }
    }

    protected void handleCandidateGroups(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            ExpressionManager expressionManager, TaskEntity taskEntity) {
        List<String> candidateGroups = humanTask.getCandidateGroups();
        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            for (String candidateGroup : candidateGroups) {
                Expression groupIdExpr = expressionManager.createExpression(candidateGroup);
                Object value = groupIdExpr.getValue(planItemInstanceEntity);
                if (value instanceof String) {
                    List<String> candidates = extractCandidates((String) value);
                    handleIdentityLinks(commandContext, taskEntity, CommandContextUtil.getIdentityLinkService().addCandidateGroups(taskEntity.getId(), candidates));
                    
                } else if (value instanceof Collection) {
                    handleIdentityLinks(commandContext, taskEntity, CommandContextUtil.getIdentityLinkService().addCandidateGroups(taskEntity.getId(), (Collection) value));
                    
                } else {
                    throw new FlowableIllegalArgumentException("Expression did not resolve to a string or collection of strings");
                }
            }
        }
    }
    
    protected void handleIdentityLinks(CommandContext commandContext, TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            if (CommandContextUtil.getCmmnEngineConfiguration().isEnableTaskRelationshipCounts()) {
                CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
                if (countingTaskEntity.isCountEnabled()) {
                    countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
                }
            }
            taskEntity.getIdentityLinks().add(identityLinkEntity);
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
        List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), VariableScopeType.CMMN);
        if (taskEntities == null || taskEntities.isEmpty()) {
            throw new FlowableException("No task entity found for plan item instance " + planItemInstance.getId());
        }
        
        // Should be only one
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isDeleted()) {
                taskService.deleteTask(taskEntity, true);
                CommandContextUtil.getCmmnHistoryManager(commandContext).recordTaskEnd(taskEntity, null);
            }
        }
        
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstance((PlanItemInstanceEntity) planItemInstance);
    }

}
