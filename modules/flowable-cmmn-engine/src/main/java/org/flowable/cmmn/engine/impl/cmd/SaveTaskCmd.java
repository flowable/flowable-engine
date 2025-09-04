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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.interceptor.CmmnIdentityLinkInterceptor;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.event.impl.FlowableTaskEventBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class SaveTaskCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected TaskEntity task;

    public SaveTaskCmd(Task task) {
        this.task = (TaskEntity) task;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (task == null) {
            throw new FlowableIllegalArgumentException("task is null");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (task.getRevision() == 0) {
            TaskHelper.insertTask(task, true, cmmnEngineConfiguration);
            cmmnEngineConfiguration.getCmmnHistoryManager().recordTaskCreated(task);

            if (CommandContextUtil.getEventDispatcher() != null && CommandContextUtil.getEventDispatcher().isEnabled()) {
                CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(
                        FlowableEngineEventType.TASK_CREATED, task), EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            }

        } else {
            
            TaskServiceConfiguration taskServiceConfiguration = cmmnEngineConfiguration.getTaskServiceConfiguration();
            TaskInfo originalTaskEntity = taskServiceConfiguration.getTaskService().getTask(task.getId());
            
            if (originalTaskEntity == null && cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                originalTaskEntity = taskServiceConfiguration.getHistoricTaskService().getHistoricTask(task.getId());
            }
            
            String originalAssignee = originalTaskEntity.getAssignee();
            
            cmmnEngineConfiguration.getCmmnHistoryManager().recordTaskInfoChange(task, cmmnEngineConfiguration.getClock().getCurrentTime());
            taskServiceConfiguration.getTaskService().updateTask(task, true);
            
            if (!StringUtils.equals(originalAssignee, task.getAssignee())) {

                CmmnIdentityLinkInterceptor identityLinkInterceptor = cmmnEngineConfiguration.getIdentityLinkInterceptor();
                if (identityLinkInterceptor != null && task.getAssignee() != null) {
                    identityLinkInterceptor.handleAddAssigneeIdentityLinkToTask(task, task.getAssignee());
                }

                cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(task, TaskListener.EVENTNAME_ASSIGNMENT);

                if (CommandContextUtil.getEventDispatcher() != null && CommandContextUtil.getEventDispatcher().isEnabled()) {
                    CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableTaskEventBuilder.createEntityEvent(
                            FlowableEngineEventType.TASK_ASSIGNED, task), EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
                }

            }

            String originalOwner = originalTaskEntity.getOwner();

            if (!StringUtils.equals(originalOwner, task.getOwner()) && task.getOwner() != null) {
                CmmnIdentityLinkInterceptor identityLinkInterceptor = cmmnEngineConfiguration.getIdentityLinkInterceptor();
                if (identityLinkInterceptor != null) {
                    identityLinkInterceptor.handleAddOwnerIdentityLinkToTask(task, task.getOwner());
                }
            }
        }

        return null;
    }

}
