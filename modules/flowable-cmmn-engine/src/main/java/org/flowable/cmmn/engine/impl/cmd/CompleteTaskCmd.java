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

import static org.flowable.cmmn.engine.impl.task.TaskHelper.logUserTaskCompleted;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class CompleteTaskCmd implements Command<Void> {
    
    protected String taskId;
    protected Map<String, Object> variables;
    protected Map<String, Object> transientVariables;
    
    public CompleteTaskCmd(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {  
        this.taskId = taskId;
        this.variables = variables;
        this.transientVariables = transientVariables;
    }
    
    @Override
    public Void execute(CommandContext commandContext) {
        
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("Null task id");
        }
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        TaskEntity taskEntity = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService().getTask(taskId);
        if (taskEntity == null) {
            throw new FlowableObjectNotFoundException("Could not find task entity for id " + taskId, TaskEntity.class);
        }
        
        if (StringUtils.isNotEmpty(taskEntity.getProcessInstanceId())) {
            throw new FlowableException("The task instance is created by the process engine and should be completed via the process engine API");
        }
        
        String planItemInstanceId = taskEntity.getSubScopeId();
        PlanItemInstanceEntity planItemInstanceEntity = null;
        if (planItemInstanceId != null) {
            planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceId);
            if (planItemInstanceEntity == null) {
                throw new FlowableException("Could not find plan item instance for task " + taskId);
            }
        }
        
        if (variables != null) {
            taskEntity.setVariables(variables);
        }
        if (transientVariables != null) {
            taskEntity.setTransientVariables(transientVariables);
        }

        logUserTaskCompleted(taskEntity, cmmnEngineConfiguration);
        
        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleCompleteTask(taskEntity);
        }

        cmmnEngineConfiguration.getListenerNotificationHelper().executeTaskListeners(taskEntity, TaskListener.EVENTNAME_COMPLETE);

        if (planItemInstanceEntity != null) {
            if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                String taskLabel = null;
                if (StringUtils.isNotEmpty(taskEntity.getName())) {
                    taskLabel = taskEntity.getName();
                } else {
                    taskLabel = taskEntity.getId();
                }
            
                CmmnLoggingSessionUtil.addLoggingData(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_COMPLETE, 
                        "Human task '" + taskLabel + "' completed", taskEntity, planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
            }
            
            CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
            
        } else {
            TaskHelper.deleteTask(taskEntity, null, false, true, cmmnEngineConfiguration);
        }
        
        return null;
    }

}
