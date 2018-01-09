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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.impl.calendar.BusinessCalendar;
import org.flowable.engine.common.impl.calendar.DueDateBusinessCalendar;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class UserTaskActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskActivityBehavior.class);

    protected UserTask userTask;

    public UserTaskActivityBehavior(UserTask userTask) {
        this.userTask = userTask;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        TaskService taskService = CommandContextUtil.getTaskService(commandContext);

        TaskEntity task = taskService.createTask();
        task.setExecutionId(execution.getId());
        task.setTaskDefinitionKey(userTask.getId());

        String activeTaskName = null;
        String activeTaskDescription = null;
        String activeTaskDueDate = null;
        String activeTaskPriority = null;
        String activeTaskCategory = null;
        String activeTaskFormKey = null;
        String activeTaskSkipExpression = null;
        String activeTaskAssignee = null;
        String activeTaskOwner = null;
        List<String> activeTaskCandidateUsers = null;
        List<String> activeTaskCandidateGroups = null;

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (CommandContextUtil.getProcessEngineConfiguration(commandContext).isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(userTask.getId(), execution.getProcessDefinitionId());
            activeTaskName = getActiveValue(userTask.getName(), DynamicBpmnConstants.USER_TASK_NAME, taskElementProperties);
            activeTaskDescription = getActiveValue(userTask.getDocumentation(), DynamicBpmnConstants.USER_TASK_DESCRIPTION, taskElementProperties);
            activeTaskDueDate = getActiveValue(userTask.getDueDate(), DynamicBpmnConstants.USER_TASK_DUEDATE, taskElementProperties);
            activeTaskPriority = getActiveValue(userTask.getPriority(), DynamicBpmnConstants.USER_TASK_PRIORITY, taskElementProperties);
            activeTaskCategory = getActiveValue(userTask.getCategory(), DynamicBpmnConstants.USER_TASK_CATEGORY, taskElementProperties);
            activeTaskFormKey = getActiveValue(userTask.getFormKey(), DynamicBpmnConstants.USER_TASK_FORM_KEY, taskElementProperties);
            activeTaskSkipExpression = getActiveValue(userTask.getSkipExpression(), DynamicBpmnConstants.TASK_SKIP_EXPRESSION, taskElementProperties);
            activeTaskAssignee = getActiveValue(userTask.getAssignee(), DynamicBpmnConstants.USER_TASK_ASSIGNEE, taskElementProperties);
            activeTaskOwner = getActiveValue(userTask.getOwner(), DynamicBpmnConstants.USER_TASK_OWNER, taskElementProperties);
            activeTaskCandidateUsers = getActiveValueList(userTask.getCandidateUsers(), DynamicBpmnConstants.USER_TASK_CANDIDATE_USERS, taskElementProperties);
            activeTaskCandidateGroups = getActiveValueList(userTask.getCandidateGroups(), DynamicBpmnConstants.USER_TASK_CANDIDATE_GROUPS, taskElementProperties);

        } else {
            activeTaskName = userTask.getName();
            activeTaskDescription = userTask.getDocumentation();
            activeTaskDueDate = userTask.getDueDate();
            activeTaskPriority = userTask.getPriority();
            activeTaskCategory = userTask.getCategory();
            activeTaskFormKey = userTask.getFormKey();
            activeTaskSkipExpression = userTask.getSkipExpression();
            activeTaskAssignee = userTask.getAssignee();
            activeTaskOwner = userTask.getOwner();
            activeTaskCandidateUsers = userTask.getCandidateUsers();
            activeTaskCandidateGroups = userTask.getCandidateGroups();
        }

        if (StringUtils.isNotEmpty(activeTaskName)) {
            String name = null;
            try {
                name = (String) expressionManager.createExpression(activeTaskName).getValue(execution);
            } catch (FlowableException e) {
                name = activeTaskName;
                LOGGER.warn("property not found in task name expression {}", e.getMessage());
            }
            task.setName(name);
        }

        if (StringUtils.isNotEmpty(activeTaskDescription)) {
            String description = null;
            try {
                description = (String) expressionManager.createExpression(activeTaskDescription).getValue(execution);
            } catch (FlowableException e) {
                description = activeTaskDescription;
                LOGGER.warn("property not found in task description expression {}", e.getMessage());
            }
            task.setDescription(description);
        }

        if (StringUtils.isNotEmpty(activeTaskDueDate)) {
            Object dueDate = expressionManager.createExpression(activeTaskDueDate).getValue(execution);
            if (dueDate != null) {
                if (dueDate instanceof Date) {
                    task.setDueDate((Date) dueDate);
                } else if (dueDate instanceof String) {
                    String businessCalendarName = null;
                    if (StringUtils.isNotEmpty(userTask.getBusinessCalendarName())) {
                        businessCalendarName = expressionManager.createExpression(userTask.getBusinessCalendarName()).getValue(execution).toString();
                    } else {
                        businessCalendarName = DueDateBusinessCalendar.NAME;
                    }

                    BusinessCalendar businessCalendar = CommandContextUtil.getProcessEngineConfiguration(commandContext).getBusinessCalendarManager()
                            .getBusinessCalendar(businessCalendarName);
                    task.setDueDate(businessCalendar.resolveDuedate((String) dueDate));

                } else {
                    throw new FlowableIllegalArgumentException("Due date expression does not resolve to a Date or Date string: " + activeTaskDueDate);
                }
            }
        }

        if (StringUtils.isNotEmpty(activeTaskPriority)) {
            final Object priority = expressionManager.createExpression(activeTaskPriority).getValue(execution);
            if (priority != null) {
                if (priority instanceof String) {
                    try {
                        task.setPriority(Integer.valueOf((String) priority));
                    } catch (NumberFormatException e) {
                        throw new FlowableIllegalArgumentException("Priority does not resolve to a number: " + priority, e);
                    }
                } else if (priority instanceof Number) {
                    task.setPriority(((Number) priority).intValue());
                } else {
                    throw new FlowableIllegalArgumentException("Priority expression does not resolve to a number: " + activeTaskPriority);
                }
            }
        }

        if (StringUtils.isNotEmpty(activeTaskCategory)) {
            final Object category = expressionManager.createExpression(activeTaskCategory).getValue(execution);
            if (category != null) {
                if (category instanceof String) {
                    task.setCategory((String) category);
                } else {
                    throw new FlowableIllegalArgumentException("Category expression does not resolve to a string: " + activeTaskCategory);
                }
            }
        }

        if (StringUtils.isNotEmpty(activeTaskFormKey)) {
            final Object formKey = expressionManager.createExpression(activeTaskFormKey).getValue(execution);
            if (formKey != null) {
                if (formKey instanceof String) {
                    task.setFormKey((String) formKey);
                } else {
                    throw new FlowableIllegalArgumentException("FormKey expression does not resolve to a string: " + activeTaskFormKey);
                }
            }
        }

        boolean skipUserTask = false;
        if (StringUtils.isNotEmpty(activeTaskSkipExpression)) {
            Expression skipExpression = expressionManager.createExpression(activeTaskSkipExpression);
            skipUserTask = SkipExpressionUtil.isSkipExpressionEnabled(execution, skipExpression)
                    && SkipExpressionUtil.shouldSkipFlowElement(execution, skipExpression);
        }
        
        TaskHelper.insertTask(task, (ExecutionEntity) execution, !skipUserTask);

        // Handling assignments need to be done after the task is inserted, to have an id
        if (!skipUserTask) {
            handleAssignments(taskService, activeTaskAssignee, activeTaskOwner,
                    activeTaskCandidateUsers, activeTaskCandidateGroups, task, expressionManager, execution);
            
            processEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_CREATE);

            // All properties set, now firing 'create' events
            if (CommandContextUtil.getTaskServiceConfiguration(commandContext).getEventDispatcher().isEnabled()) {
                CommandContextUtil.getTaskServiceConfiguration(commandContext).getEventDispatcher().dispatchEvent(
                        FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task));
            }
            
        } else {
            TaskHelper.deleteTask(task, null, false, false);
            leave(execution);
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        List<TaskEntity> taskEntities = CommandContextUtil.getTaskService().findTasksByExecutionId(execution.getId()); // Should be only one
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isDeleted()) {
                throw new FlowableException("UserTask should not be signalled before complete");
            }
        }

        leave(execution);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleAssignments(TaskService taskService, String assignee, String owner, List<String> candidateUsers,
            List<String> candidateGroups, TaskEntity task, ExpressionManager expressionManager, DelegateExecution execution) {

        if (StringUtils.isNotEmpty(assignee)) {
            Object assigneeExpressionValue = expressionManager.createExpression(assignee).getValue(execution);
            String assigneeValue = null;
            if (assigneeExpressionValue != null) {
                assigneeValue = assigneeExpressionValue.toString();
            }

            if (StringUtils.isNotEmpty(assigneeValue)) {
                TaskHelper.changeTaskAssignee(task, assigneeValue);
            }
        }

        if (StringUtils.isNotEmpty(owner)) {
            Object ownerExpressionValue = expressionManager.createExpression(owner).getValue(execution);
            String ownerValue = null;
            if (ownerExpressionValue != null) {
                ownerValue = ownerExpressionValue.toString();
            }

            if (StringUtils.isNotEmpty(ownerValue)) {
                TaskHelper.changeTaskOwner(task, ownerValue);
            }
        }

        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            for (String candidateGroup : candidateGroups) {
                Expression groupIdExpr = expressionManager.createExpression(candidateGroup);
                Object value = groupIdExpr.getValue(execution);
                if (value != null) {
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (StringUtils.isNotEmpty(strValue)) {
                            List<String> candidates = extractCandidates(strValue);
                            List<IdentityLinkEntity> identityLinkEntities = CommandContextUtil.getIdentityLinkService().addCandidateGroups(task.getId(), candidates);
                            IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        }
                        
                    } else if (value instanceof Collection) {
                        List<IdentityLinkEntity> identityLinkEntities = CommandContextUtil.getIdentityLinkService().addCandidateGroups(task.getId(), (Collection) value);
                        IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        
                    } else {
                        throw new FlowableIllegalArgumentException("Expression did not resolve to a string or collection of strings");
                    }
                }
            }
        }

        if (candidateUsers != null && !candidateUsers.isEmpty()) {
            for (String candidateUser : candidateUsers) {
                Expression userIdExpr = expressionManager.createExpression(candidateUser);
                Object value = userIdExpr.getValue(execution);
                if (value != null) {
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (StringUtils.isNotEmpty(strValue)) {
                            List<String> candidates = extractCandidates(strValue);
                            List<IdentityLinkEntity> identityLinkEntities = CommandContextUtil.getIdentityLinkService().addCandidateUsers(task.getId(), candidates);
                            IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        }
                        
                    } else if (value instanceof Collection) {
                        List<IdentityLinkEntity> identityLinkEntities = CommandContextUtil.getIdentityLinkService().addCandidateUsers(task.getId(), (Collection) value);
                        IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        
                    } else {
                        throw new FlowableException("Expression did not resolve to a string or collection of strings");
                    }
                }
            }
        }

        if (userTask.getCustomUserIdentityLinks() != null && !userTask.getCustomUserIdentityLinks().isEmpty()) {

            for (String customUserIdentityLinkType : userTask.getCustomUserIdentityLinks().keySet()) {
                for (String userIdentityLink : userTask.getCustomUserIdentityLinks().get(customUserIdentityLinkType)) {
                    Expression idExpression = expressionManager.createExpression(userIdentityLink);
                    Object value = idExpression.getValue(execution);
                    if (value instanceof String) {
                        List<String> userIds = extractCandidates((String) value);
                        for (String userId : userIds) {
                            IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createTaskIdentityLink(task.getId(), userId, null, customUserIdentityLinkType);
                            IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        }
                    } else if (value instanceof Collection) {
                        Iterator userIdSet = ((Collection) value).iterator();
                        while (userIdSet.hasNext()) {
                            IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createTaskIdentityLink(
                                            task.getId(), (String) userIdSet.next(), null, customUserIdentityLinkType);
                            IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        }
                    } else {
                        throw new FlowableException("Expression did not resolve to a string or collection of strings");
                    }

                }
            }

        }

        if (userTask.getCustomGroupIdentityLinks() != null && !userTask.getCustomGroupIdentityLinks().isEmpty()) {

            for (String customGroupIdentityLinkType : userTask.getCustomGroupIdentityLinks().keySet()) {
                for (String groupIdentityLink : userTask.getCustomGroupIdentityLinks().get(customGroupIdentityLinkType)) {

                    Expression idExpression = expressionManager.createExpression(groupIdentityLink);
                    Object value = idExpression.getValue(execution);
                    if (value instanceof String) {
                        List<String> groupIds = extractCandidates((String) value);
                        for (String groupId : groupIds) {
                            IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createTaskIdentityLink(
                                            task.getId(), null, groupId, customGroupIdentityLinkType);
                            IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        }
                    } else if (value instanceof Collection) {
                        Iterator groupIdSet = ((Collection) value).iterator();
                        while (groupIdSet.hasNext()) {
                            IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createTaskIdentityLink(
                                            task.getId(), null, (String) groupIdSet.next(), customGroupIdentityLinkType);
                            IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        }
                    } else {
                        throw new FlowableException("Expression did not resolve to a string or collection of strings");
                    }

                }
            }

        }

    }

    /**
     * Extract a candidate list from a string.
     * 
     * @param str
     * @return
     */
    protected List<String> extractCandidates(String str) {
        return Arrays.asList(str.split("[\\s]*,[\\s]*"));
    }
}