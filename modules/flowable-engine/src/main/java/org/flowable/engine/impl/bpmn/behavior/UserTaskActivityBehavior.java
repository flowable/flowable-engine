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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.helper.DynamicPropertyUtil;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.delegate.ActivityWithMigrationContextBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.interceptor.CreateUserTaskAfterContext;
import org.flowable.engine.interceptor.CreateUserTaskBeforeContext;
import org.flowable.engine.interceptor.MigrationContext;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class UserTaskActivityBehavior extends TaskActivityBehavior implements ActivityWithMigrationContextBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskActivityBehavior.class);

    protected UserTask userTask;

    public UserTaskActivityBehavior(UserTask userTask) {
        this.userTask = userTask;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execute(execution, null);
    }
    
    @Override
    public void execute(DelegateExecution execution, MigrationContext migrationContext) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskService taskService = processEngineConfiguration.getTaskServiceConfiguration().getTaskService();

        TaskEntity task = taskService.createTask();
        task.setExecutionId(execution.getId());
        task.setTaskDefinitionKey(userTask.getId());
        task.setPropagatedStageInstanceId(execution.getPropagatedStageInstanceId());

        String activeTaskName = null;
        String activeTaskDescription = null;
        String activeTaskDueDate = null;
        String activeTaskPriority = null;
        String activeTaskCategory = null;
        String activeTaskFormKey = null;
        String activeTaskSkipExpression = null;
        String activeTaskAssignee = null;
        String activeTaskOwner = null;
        String activeTaskIdVariableName = null;
        List<String> activeTaskCandidateUsers = null;
        List<String> activeTaskCandidateGroups = null;

        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

        if (processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(userTask.getId(), execution.getProcessDefinitionId());
            activeTaskName = DynamicPropertyUtil.getActiveValue(userTask.getName(), DynamicBpmnConstants.USER_TASK_NAME, taskElementProperties);
            activeTaskDescription = DynamicPropertyUtil.getActiveValue(userTask.getDocumentation(), DynamicBpmnConstants.USER_TASK_DESCRIPTION, taskElementProperties);
            activeTaskDueDate = DynamicPropertyUtil.getActiveValue(userTask.getDueDate(), DynamicBpmnConstants.USER_TASK_DUEDATE, taskElementProperties);
            activeTaskPriority = DynamicPropertyUtil.getActiveValue(userTask.getPriority(), DynamicBpmnConstants.USER_TASK_PRIORITY, taskElementProperties);
            activeTaskCategory = DynamicPropertyUtil.getActiveValue(userTask.getCategory(), DynamicBpmnConstants.USER_TASK_CATEGORY, taskElementProperties);
            activeTaskFormKey = DynamicPropertyUtil.getActiveValue(userTask.getFormKey(), DynamicBpmnConstants.USER_TASK_FORM_KEY, taskElementProperties);
            activeTaskSkipExpression = DynamicPropertyUtil.getActiveValue(userTask.getSkipExpression(), DynamicBpmnConstants.TASK_SKIP_EXPRESSION, taskElementProperties);
            activeTaskAssignee = getAssigneeValue(userTask, migrationContext, taskElementProperties);
            activeTaskOwner = getOwnerValue(userTask, migrationContext, taskElementProperties);
            activeTaskCandidateUsers = getActiveValueList(userTask.getCandidateUsers(), DynamicBpmnConstants.USER_TASK_CANDIDATE_USERS, taskElementProperties);
            activeTaskCandidateGroups = getActiveValueList(userTask.getCandidateGroups(), DynamicBpmnConstants.USER_TASK_CANDIDATE_GROUPS, taskElementProperties);
            activeTaskIdVariableName = DynamicPropertyUtil.getActiveValue(userTask.getTaskIdVariableName(), DynamicBpmnConstants.USER_TASK_TASK_ID_VARIABLE_NAME, taskElementProperties);
        } else {
            activeTaskName = userTask.getName();
            activeTaskDescription = userTask.getDocumentation();
            activeTaskDueDate = userTask.getDueDate();
            activeTaskPriority = userTask.getPriority();
            activeTaskCategory = userTask.getCategory();
            activeTaskFormKey = userTask.getFormKey();
            activeTaskSkipExpression = userTask.getSkipExpression();
            activeTaskAssignee = getAssigneeValue(userTask, migrationContext, null);
            activeTaskOwner = getOwnerValue(userTask, migrationContext, null);
            activeTaskCandidateUsers = userTask.getCandidateUsers();
            activeTaskCandidateGroups = userTask.getCandidateGroups();
            activeTaskIdVariableName = userTask.getTaskIdVariableName();
        }
        if (execution.getCurrentActivityName() != null) {
            activeTaskName = execution.getCurrentActivityName();
        }
        
        CreateUserTaskBeforeContext beforeContext = new CreateUserTaskBeforeContext(userTask, execution, activeTaskName, activeTaskDescription, activeTaskDueDate, 
                        activeTaskPriority, activeTaskCategory, activeTaskFormKey, activeTaskSkipExpression, activeTaskAssignee, activeTaskOwner, 
                        activeTaskCandidateUsers, activeTaskCandidateGroups);
        
        if (processEngineConfiguration.getCreateUserTaskInterceptor() != null) {
            processEngineConfiguration.getCreateUserTaskInterceptor().beforeCreateUserTask(beforeContext);
        }

        handleName(beforeContext, expressionManager, task, execution);
        handleDescription(beforeContext, expressionManager, task, execution);
        handleDueDate(beforeContext, expressionManager, task, execution, processEngineConfiguration, activeTaskDueDate);
        handlePriority(beforeContext, expressionManager, task, execution, activeTaskPriority);
        handleCategory(beforeContext, expressionManager, task, execution);
        handleFormKey(beforeContext, expressionManager, task, execution);
        
        boolean skipUserTask = SkipExpressionUtil.isSkipExpressionEnabled(beforeContext.getSkipExpression(), userTask.getId(), execution, commandContext)
                    && SkipExpressionUtil.shouldSkipFlowElement(beforeContext.getSkipExpression(), userTask.getId(), execution, commandContext);

        TaskHelper.insertTask(task, (ExecutionEntity) execution, !skipUserTask, (!skipUserTask && processEngineConfiguration.isEnableEntityLinks()));

        // Handling assignments need to be done after the task is inserted, to have an id
        if (!skipUserTask) {
            if (processEngineConfiguration.isLoggingSessionEnabled()) {
                BpmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_USER_TASK_CREATE, "User task '" + 
                                task.getName() + "' created", task, execution);
            }
            
            handleAssignments(taskService, beforeContext.getAssignee(), beforeContext.getOwner(), beforeContext.getCandidateUsers(), 
                            beforeContext.getCandidateGroups(), task, expressionManager, execution, processEngineConfiguration);
            
            if (processEngineConfiguration.getCreateUserTaskInterceptor() != null) {
                CreateUserTaskAfterContext afterContext = new CreateUserTaskAfterContext(userTask, task, execution);
                processEngineConfiguration.getCreateUserTaskInterceptor().afterCreateUserTask(afterContext);
            }

            try {
                processEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_CREATE);
            } catch (BpmnError bpmnError) {
                ErrorPropagation.propagateError(bpmnError, execution);
                return;
            }

            // All properties set, now firing 'create' events
            FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getTaskServiceConfiguration().getEventDispatcher();
            if (eventDispatcher != null  && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(FlowableEngineEventType.TASK_CREATED, task),
                        processEngineConfiguration.getEngineCfgKey());
            }
            
            if (StringUtils.isNotEmpty(activeTaskIdVariableName)) {
                Expression expression = expressionManager.createExpression(userTask.getTaskIdVariableName());
                String idVariableName = (String) expression.getValue(execution);
                if (StringUtils.isNotEmpty(idVariableName)) {
                    execution.setVariable(idVariableName, task.getId());
                }
            }
        } else {
            TaskHelper.deleteTask(task, null, false, false, false); // false: no events fired for skipped user task
            leave(execution);
        }

    }

    protected void handleName(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task, DelegateExecution execution) {
        if (StringUtils.isNotEmpty(beforeContext.getName())) {
            String name = null;
            try {
                Object nameValue = expressionManager.createExpression(beforeContext.getName()).getValue(execution);
                if (nameValue != null) {
                    name = nameValue.toString();
                }
            } catch (FlowableException e) {
                name = beforeContext.getName();
                LOGGER.warn("property not found in task name expression {}", e.getMessage());
            }
            task.setName(name);
        }
    }

    protected void handleDescription(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task,
            DelegateExecution execution) {
        if (StringUtils.isNotEmpty(beforeContext.getDescription())) {
            String description = null;
            try {
                Object descriptionValue = expressionManager.createExpression(beforeContext.getDescription()).getValue(execution);
                if (descriptionValue != null) {
                    description = descriptionValue.toString();
                }
            } catch (FlowableException e) {
                description = beforeContext.getDescription();
                LOGGER.warn("property not found in task description expression {}", e.getMessage());
            }
            task.setDescription(description);
        }
    }

    protected void handleDueDate(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task, DelegateExecution execution,
            ProcessEngineConfigurationImpl processEngineConfiguration, String activeTaskDueDate) {
        if (StringUtils.isNotEmpty(beforeContext.getDueDate())) {
            Object dueDate = expressionManager.createExpression(beforeContext.getDueDate()).getValue(execution);
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

                    BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager()
                            .getBusinessCalendar(businessCalendarName);
                    task.setDueDate(businessCalendar.resolveDuedate((String) dueDate));
                } else if (dueDate instanceof Instant) {
                    task.setDueDate(Date.from((Instant) dueDate));
                } else if (dueDate instanceof LocalDate) {
                    Date localDueDate = Date.from(((LocalDate) dueDate).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
                    task.setDueDate(localDueDate);
                } else if (dueDate instanceof LocalDateTime) {
                    Date localDueDate = Date.from(((LocalDateTime) dueDate).atZone(ZoneId.systemDefault()).toInstant());
                    task.setDueDate(localDueDate);
                } else {
                    throw new FlowableIllegalArgumentException("Due date expression does not resolve to a Date, Instant, LocalDate, LocalDateTime or Date string: " + beforeContext.getDueDate());
                }
            }
        }
    }

    protected void handlePriority(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task, DelegateExecution execution,
            String activeTaskPriority) {
        if (StringUtils.isNotEmpty(beforeContext.getPriority())) {
            final Object priority = expressionManager.createExpression(beforeContext.getPriority()).getValue(execution);
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
    }

    protected void handleCategory(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task,
            DelegateExecution execution) {
        if (StringUtils.isNotEmpty(beforeContext.getCategory())) {
            String category = null;
            try {
                Object categoryValue = expressionManager.createExpression(beforeContext.getCategory()).getValue(execution);
                if (categoryValue != null) {
                    category = categoryValue.toString();
                }
            } catch (FlowableException e) {
                category = beforeContext.getCategory();
                LOGGER.warn("property not found in task category expression {}", e.getMessage());
            }
            task.setCategory(category);
        }
    }

    protected void handleFormKey(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager, TaskEntity task, DelegateExecution execution) {
        if (StringUtils.isNotEmpty(beforeContext.getFormKey())) {
            String formKey = null;
            try {
                Object formKeyValue = expressionManager.createExpression(beforeContext.getFormKey()).getValue(execution);
                if (formKeyValue != null) {
                    formKey = formKeyValue.toString();
                }
            } catch (FlowableException e) {
                formKey = beforeContext.getFormKey();
                LOGGER.warn("property not found in task formKey expression {}", e.getMessage());
            }
            task.setFormKey(formKey);
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        List<TaskEntity> taskEntities = processEngineConfiguration.getTaskServiceConfiguration().getTaskService()
                .findTasksByExecutionId(execution.getId()); // Should be only one
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isDeleted()) {
                throw new FlowableException("UserTask should not be signalled before complete for " + taskEntity);
            }
        }

        leave(execution);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void handleAssignments(TaskService taskService, String assignee, String owner, List<String> candidateUsers,
            List<String> candidateGroups, TaskEntity task, ExpressionManager expressionManager, DelegateExecution execution, 
            ProcessEngineConfigurationImpl processEngineConfiguration) {

        if (StringUtils.isNotEmpty(assignee)) {
            Object assigneeExpressionValue = expressionManager.createExpression(assignee).getValue(execution);
            String assigneeValue = null;
            if (assigneeExpressionValue != null) {
                assigneeValue = assigneeExpressionValue.toString();
            }

            if (StringUtils.isNotEmpty(assigneeValue)) {
                TaskHelper.changeTaskAssignee(task, assigneeValue);
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    ObjectNode loggingNode = BpmnLoggingSessionUtil.fillBasicTaskLoggingData("Set task assignee value to " + assigneeValue, task, execution);
                    loggingNode.put("taskAssignee", assigneeValue);
                    LoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE, loggingNode, ScopeTypes.BPMN);
                }
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
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    ObjectNode loggingNode = BpmnLoggingSessionUtil.fillBasicTaskLoggingData("Set task owner value to " + ownerValue, task, execution);
                    loggingNode.put("taskOwner", ownerValue);
                    LoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER, loggingNode, ScopeTypes.BPMN);
                }
            }
        }

        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            List<IdentityLinkEntity> allIdentityLinkEntities = new ArrayList<>();
            for (String candidateGroup : candidateGroups) {
                Expression groupIdExpr = expressionManager.createExpression(candidateGroup);
                Object value = groupIdExpr.getValue(execution);
                if (value != null) {
                    Collection<String> candidates = extractCandidates(value);
                    List<IdentityLinkEntity> identityLinkEntities = processEngineConfiguration.getIdentityLinkServiceConfiguration()
                            .getIdentityLinkService().addCandidateGroups(task.getId(), candidates);

                    if (identityLinkEntities != null && !identityLinkEntities.isEmpty()) {
                        IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        allIdentityLinkEntities.addAll(identityLinkEntities);
                    }
                }
            }
            
            if (!allIdentityLinkEntities.isEmpty()) {
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    BpmnLoggingSessionUtil.addTaskIdentityLinkData(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, 
                            "Added " + allIdentityLinkEntities.size() + " candidate group identity links to task", false,
                            allIdentityLinkEntities, task, execution);
                }
            }
        }

        if (candidateUsers != null && !candidateUsers.isEmpty()) {
            List<IdentityLinkEntity> allIdentityLinkEntities = new ArrayList<>();
            for (String candidateUser : candidateUsers) {
                Expression userIdExpr = expressionManager.createExpression(candidateUser);
                Object value = userIdExpr.getValue(execution);
                if (value != null) {
                    Collection<String> candidates = extractCandidates(value);
                    List<IdentityLinkEntity> identityLinkEntities = processEngineConfiguration.getIdentityLinkServiceConfiguration()
                            .getIdentityLinkService().addCandidateUsers(task.getId(), candidates);

                    if (identityLinkEntities != null && !identityLinkEntities.isEmpty()) {
                        IdentityLinkUtil.handleTaskIdentityLinkAdditions(task, identityLinkEntities);
                        allIdentityLinkEntities.addAll(identityLinkEntities);
                    }
                }
            }
            
            if (!allIdentityLinkEntities.isEmpty()) {
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    BpmnLoggingSessionUtil.addTaskIdentityLinkData(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, 
                                    "Added " + allIdentityLinkEntities.size() + " candidate user identity links to task", true,
                                    allIdentityLinkEntities, task, execution);
                }
            }
        }

        if (userTask.getCustomUserIdentityLinks() != null && !userTask.getCustomUserIdentityLinks().isEmpty()) {

            List<IdentityLinkEntity> customIdentityLinkEntities = new ArrayList<>();
            for (String customUserIdentityLinkType : userTask.getCustomUserIdentityLinks().keySet()) {
                for (String userIdentityLink : userTask.getCustomUserIdentityLinks().get(customUserIdentityLinkType)) {
                    Expression idExpression = expressionManager.createExpression(userIdentityLink);
                    Object value = idExpression.getValue(execution);

                    Collection<String> userIds = extractCandidates(value);
                    for (String userId : userIds) {
                        IdentityLinkEntity identityLinkEntity = processEngineConfiguration.getIdentityLinkServiceConfiguration()
                                .getIdentityLinkService().createTaskIdentityLink(task.getId(), userId, null, customUserIdentityLinkType);
                        IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        customIdentityLinkEntities.add(identityLinkEntity);
                    }
                }
            }

            if (!customIdentityLinkEntities.isEmpty()) {
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    BpmnLoggingSessionUtil.addTaskIdentityLinkData(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, 
                                    "Added " + customIdentityLinkEntities.size() + " custom user identity links to task", true,
                                    customIdentityLinkEntities, task, execution);
                }
            }
        }

        if (userTask.getCustomGroupIdentityLinks() != null && !userTask.getCustomGroupIdentityLinks().isEmpty()) {

            List<IdentityLinkEntity> customIdentityLinkEntities = new ArrayList<>();
            for (String customGroupIdentityLinkType : userTask.getCustomGroupIdentityLinks().keySet()) {
                for (String groupIdentityLink : userTask.getCustomGroupIdentityLinks().get(customGroupIdentityLinkType)) {

                    Expression idExpression = expressionManager.createExpression(groupIdentityLink);
                    Object value = idExpression.getValue(execution);
                    Collection<String> groupIds = extractCandidates(value);
                    for (String groupId : groupIds) {
                        IdentityLinkEntity identityLinkEntity = processEngineConfiguration.getIdentityLinkServiceConfiguration()
                                .getIdentityLinkService().createTaskIdentityLink(
                                task.getId(), null, groupId, customGroupIdentityLinkType);
                        IdentityLinkUtil.handleTaskIdentityLinkAddition(task, identityLinkEntity);
                        customIdentityLinkEntities.add(identityLinkEntity);
                    }
                }
            }

            if (!customIdentityLinkEntities.isEmpty()) {
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    BpmnLoggingSessionUtil.addTaskIdentityLinkData(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, 
                                    "Added " + customIdentityLinkEntities.size() + " custom group identity links to task", false,
                                    customIdentityLinkEntities, task, execution);
                }
            }
        }

    }

    protected Collection<String> extractCandidates(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (value instanceof ArrayNode valueArrayNode) {
            Collection<String> candidates = new ArrayList<>(valueArrayNode.size());
            for (JsonNode node : valueArrayNode) {
                candidates.add(node.asText());
            }

            return candidates;
        } else if (value != null) {
            String str = value.toString();
            if (StringUtils.isNotEmpty(str)) {
                return Arrays.asList(value.toString().split("[\\s]*,[\\s]*"));
            }
        }

        return Collections.emptyList();

    }
    
    protected String getAssigneeValue(UserTask userTask, MigrationContext migrationContext, ObjectNode taskElementProperties) {
        if (migrationContext != null && migrationContext.getAssignee() != null) {
            return migrationContext.getAssignee();
            
        } else if (taskElementProperties != null) {
            return DynamicPropertyUtil.getActiveValue(userTask.getAssignee(), DynamicBpmnConstants.USER_TASK_ASSIGNEE, taskElementProperties);
        
        } else {
            return userTask.getAssignee();
        }
    }

    protected String getOwnerValue(UserTask userTask, MigrationContext migrationContext, ObjectNode taskElementProperties) {
        if (migrationContext != null && migrationContext.getOwner() != null) {
            return migrationContext.getOwner();

        } else if (taskElementProperties != null) {
            return DynamicPropertyUtil.getActiveValue(userTask.getOwner(), DynamicBpmnConstants.USER_TASK_OWNER, taskElementProperties);

        } else {
            return userTask.getOwner();
        }
    }
}
