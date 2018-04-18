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
package org.flowable.cmmn.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.AddIdentityLinkCmd;
import org.flowable.cmmn.engine.impl.cmd.ClaimTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.CompleteTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.CompleteTaskWithFormCmd;
import org.flowable.cmmn.engine.impl.cmd.DelegateTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteIdentityLinkCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.GetIdentityLinksForTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.GetSubTasksCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTaskFormModelCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTaskVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTaskVariableInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTaskVariableInstancesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTaskVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.GetTasksLocalVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.HasTaskVariableCmd;
import org.flowable.cmmn.engine.impl.cmd.NewTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.RemoveTaskVariablesCmd;
import org.flowable.cmmn.engine.impl.cmd.ResolveTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.SaveTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.SetTaskDueDateCmd;
import org.flowable.cmmn.engine.impl.cmd.SetTaskPriorityCmd;
import org.flowable.cmmn.engine.impl.cmd.SetTaskVariablesCmd;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public class CmmnTaskServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnTaskService {

    public CmmnTaskServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public Task newTask() {
        return newTask(null);
    }
    
    @Override
    public Task newTask(String taskId) {
        return commandExecutor.execute(new NewTaskCmd(taskId));
    }

    @Override
    public void saveTask(Task task) {
        commandExecutor.execute(new SaveTaskCmd(task));
    }
    
    @Override
    public void claim(String taskId, String userId) {
        commandExecutor.execute(new ClaimTaskCmd(taskId, userId));
    }

    @Override
    public void unclaim(String taskId) {
        commandExecutor.execute(new ClaimTaskCmd(taskId, null));
    }

    @Override
    public void complete(String taskId) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, null, null));
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, variables, null));        
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        commandExecutor.execute(new CompleteTaskCmd(taskId, variables, transientVariables));        
    }
    
    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables) {
        commandExecutor.execute(new CompleteTaskWithFormCmd(taskId, formDefinitionId, outcome, variables));
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, Map<String, Object> transientVariables) {

        commandExecutor.execute(new CompleteTaskWithFormCmd(taskId, formDefinitionId, outcome, variables, transientVariables));
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome,
            Map<String, Object> variables, boolean localScope) {

        commandExecutor.execute(new CompleteTaskWithFormCmd(taskId, formDefinitionId, outcome, variables, localScope));
    }
    
    @Override
    public void delegateTask(String taskId, String userId) {
        commandExecutor.execute(new DelegateTaskCmd(taskId, userId));
    }

    @Override
    public void resolveTask(String taskId) {
        commandExecutor.execute(new ResolveTaskCmd(taskId, null));
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables) {
        commandExecutor.execute(new ResolveTaskCmd(taskId, variables));
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        commandExecutor.execute(new ResolveTaskCmd(taskId, variables, transientVariables));
    }
    
    @Override
    public void deleteTask(String taskId) {
        commandExecutor.execute(new DeleteTaskCmd(taskId, null, false));
    }

    @Override
    public void deleteTasks(Collection<String> taskIds) {
        commandExecutor.execute(new DeleteTaskCmd(taskIds, null, false));
    }

    @Override
    public void deleteTask(String taskId, boolean cascade) {
        commandExecutor.execute(new DeleteTaskCmd(taskId, null, cascade));
    }

    @Override
    public void deleteTasks(Collection<String> taskIds, boolean cascade) {
        commandExecutor.execute(new DeleteTaskCmd(taskIds, null, cascade));
    }

    @Override
    public void deleteTask(String taskId, String deleteReason) {
        commandExecutor.execute(new DeleteTaskCmd(taskId, deleteReason, false));
    }
    
    @Override
    public void deleteTasks(Collection<String> taskIds, String deleteReason) {
        commandExecutor.execute(new DeleteTaskCmd(taskIds, deleteReason, false));
    }

    @Override
    public FormInfo getTaskFormModel(String taskId) {
        return commandExecutor.execute(new GetTaskFormModelCmd(taskId));
    }
    
    @Override
    public void setPriority(String taskId, int priority) {
        commandExecutor.execute(new SetTaskPriorityCmd(taskId, priority));
    }

    @Override
    public void setDueDate(String taskId, Date dueDate) {
        commandExecutor.execute(new SetTaskDueDateCmd(taskId, dueDate));
    }
    
    @Override
    public TaskQuery createTaskQuery() {
        return new TaskQueryImpl(commandExecutor);
    }
    
    @Override
    public List<Task> getSubTasks(String parentTaskId) {
        return commandExecutor.execute(new GetSubTasksCmd(parentTaskId));
    }
    
    @Override
    public Map<String, Object> getVariables(String taskId) {
        return commandExecutor.execute(new GetTaskVariablesCmd(taskId, null, false));
    }

    @Override
    public Map<String, Object> getVariablesLocal(String taskId) {
        return commandExecutor.execute(new GetTaskVariablesCmd(taskId, null, true));
    }

    @Override
    public Map<String, Object> getVariables(String taskId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetTaskVariablesCmd(taskId, variableNames, false));
    }

    @Override
    public Map<String, Object> getVariablesLocal(String taskId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetTaskVariablesCmd(taskId, variableNames, true));
    }

    @Override
    public Object getVariable(String taskId, String variableName) {
        return commandExecutor.execute(new GetTaskVariableCmd(taskId, variableName, false));
    }

    @Override
    public <T> T getVariable(String taskId, String variableName, Class<T> variableClass) {
        return variableClass.cast(getVariable(taskId, variableName));
    }

    @Override
    public boolean hasVariable(String taskId, String variableName) {
        return commandExecutor.execute(new HasTaskVariableCmd(taskId, variableName, false));
    }

    @Override
    public Object getVariableLocal(String taskId, String variableName) {
        return commandExecutor.execute(new GetTaskVariableCmd(taskId, variableName, true));
    }

    @Override
    public <T> T getVariableLocal(String taskId, String variableName, Class<T> variableClass) {
        return variableClass.cast(getVariableLocal(taskId, variableName));
    }

    @Override
    public List<VariableInstance> getVariableInstancesLocalByTaskIds(Set<String> taskIds) {
        return commandExecutor.execute(new GetTasksLocalVariablesCmd(taskIds));
    }

    @Override
    public boolean hasVariableLocal(String taskId, String variableName) {
        return commandExecutor.execute(new HasTaskVariableCmd(taskId, variableName, true));
    }

    @Override
    public void setVariable(String taskId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetTaskVariablesCmd(taskId, variables, false));
    }

    @Override
    public void setVariableLocal(String taskId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetTaskVariablesCmd(taskId, variables, true));
    }

    @Override
    public void setVariables(String taskId, Map<String, ? extends Object> variables) {
        commandExecutor.execute(new SetTaskVariablesCmd(taskId, variables, false));
    }

    @Override
    public void setVariablesLocal(String taskId, Map<String, ? extends Object> variables) {
        commandExecutor.execute(new SetTaskVariablesCmd(taskId, variables, true));
    }

    @Override
    public void removeVariable(String taskId, String variableName) {
        Collection<String> variableNames = new ArrayList<>();
        variableNames.add(variableName);
        commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
    }

    @Override
    public void removeVariableLocal(String taskId, String variableName) {
        Collection<String> variableNames = new ArrayList<>(1);
        variableNames.add(variableName);
        commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
    }

    @Override
    public void removeVariables(String taskId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, false));
    }

    @Override
    public void removeVariablesLocal(String taskId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveTaskVariablesCmd(taskId, variableNames, true));
    }
    
    @Override
    public VariableInstance getVariableInstance(String taskId, String variableName) {
        return commandExecutor.execute(new GetTaskVariableInstanceCmd(taskId, variableName, false));
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String taskId, String variableName) {
        return commandExecutor.execute(new GetTaskVariableInstanceCmd(taskId, variableName, true));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String taskId) {
        return commandExecutor.execute(new GetTaskVariableInstancesCmd(taskId, null, false));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String taskId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetTaskVariableInstancesCmd(taskId, variableNames, false));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String taskId) {
        return commandExecutor.execute(new GetTaskVariableInstancesCmd(taskId, null, true));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String taskId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetTaskVariableInstancesCmd(taskId, variableNames, true));
    }
    
    @Override
    public void setAssignee(String taskId, String userId) {
        commandExecutor.execute(new AddIdentityLinkCmd(taskId, userId, AddIdentityLinkCmd.IDENTITY_USER, IdentityLinkType.ASSIGNEE));
    }

    @Override
    public void setOwner(String taskId, String userId) {
        commandExecutor.execute(new AddIdentityLinkCmd(taskId, userId, AddIdentityLinkCmd.IDENTITY_USER, IdentityLinkType.OWNER));
    }
    
    @Override
    public void addUserIdentityLink(String taskId, String userId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkCmd(taskId, userId, AddIdentityLinkCmd.IDENTITY_USER, identityLinkType));
    }

    @Override
    public void addGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkCmd(taskId, groupId, AddIdentityLinkCmd.IDENTITY_GROUP, identityLinkType));
    }
    
    @Override
    public void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkCmd(taskId, null, groupId, identityLinkType));
    }

    @Override
    public void deleteUserIdentityLink(String taskId, String userId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkCmd(taskId, userId, null, identityLinkType));
    }

    @Override
    public List<IdentityLink> getIdentityLinksForTask(String taskId) {
        return commandExecutor.execute(new GetIdentityLinksForTaskCmd(taskId));
    }

    @Override
    public TaskBuilder createTaskBuilder() {
        return new CmmnTaskBuilderImpl(commandExecutor);
    }

}
