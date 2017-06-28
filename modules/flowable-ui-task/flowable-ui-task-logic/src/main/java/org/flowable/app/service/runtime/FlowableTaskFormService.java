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
package org.flowable.app.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.app.model.runtime.CompleteFormRepresentation;
import org.flowable.app.model.runtime.ProcessInstanceVariableRepresentation;
import org.flowable.app.model.runtime.SaveFormRepresentation;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.service.exception.NotPermittedException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.task.Task;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.model.FormModel;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableTaskFormService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableTaskFormService.class);

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected FormService formService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected ObjectMapper objectMapper;

    public FormModel getTaskForm(String taskId) {
        HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
        return taskService.getTaskFormModel(task.getId());
    }

    public void saveTaskForm(String taskId, SaveFormRepresentation saveFormRepresentation) {

        // Get the form definition
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task not found with id: " + taskId);
        }

        checkCurrentUserCanModifyTask(task);

        formService.saveFormInstanceByFormModelId(saveFormRepresentation.getValues(), saveFormRepresentation.getFormId(), taskId, task.getProcessInstanceId());

    }

    public void completeTaskForm(String taskId, CompleteFormRepresentation completeTaskFormRepresentation) {

        // Get the form definition
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new NotFoundException("Task not found with id: " + taskId);
        }

        checkCurrentUserCanModifyTask(task);

        taskService.completeTaskWithForm(taskId, completeTaskFormRepresentation.getFormId(),
                completeTaskFormRepresentation.getOutcome(), completeTaskFormRepresentation.getValues());
    }

    public List<ProcessInstanceVariableRepresentation> getProcessInstanceVariables(String taskId) {
        HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
        List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();

        // Get all process-variables to extract values from
        Map<String, ProcessInstanceVariableRepresentation> processInstanceVariables = new HashMap<String, ProcessInstanceVariableRepresentation>();

        for (HistoricVariableInstance historicVariableInstance : historicVariables) {
            ProcessInstanceVariableRepresentation processInstanceVariableRepresentation = new ProcessInstanceVariableRepresentation(
                    historicVariableInstance.getVariableName(), historicVariableInstance.getVariableTypeName(), historicVariableInstance.getValue());
            processInstanceVariables.put(historicVariableInstance.getId(), processInstanceVariableRepresentation);
        }

        List<ProcessInstanceVariableRepresentation> processInstanceVariableRepresenations = new ArrayList<ProcessInstanceVariableRepresentation>(processInstanceVariables.values());
        return processInstanceVariableRepresenations;
    }

    private void checkCurrentUserCanModifyTask(Task task) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        if (!permissionService.isTaskOwnerOrAssignee(currentUser, task.getId())) {
            if (!permissionService.validateIfUserIsInitiatorAndCanCompleteTask(currentUser, task)) {
                throw new NotPermittedException();
            }
        }
    }
}
