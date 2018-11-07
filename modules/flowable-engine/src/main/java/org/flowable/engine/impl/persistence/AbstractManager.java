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

package org.flowable.engine.impl.persistence;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.ModelEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class AbstractManager {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public AbstractManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped

    protected ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    protected CommandExecutor getCommandExecutor() {
        return getProcessEngineConfiguration().getCommandExecutor();
    }

    protected Clock getClock() {
        return getProcessEngineConfiguration().getClock();
    }

    protected AsyncExecutor getAsyncExecutor() {
        return getProcessEngineConfiguration().getAsyncExecutor();
    }

    protected FlowableEventDispatcher getEventDispatcher() {
        return getProcessEngineConfiguration().getEventDispatcher();
    }

    protected HistoryManager getHistoryManager() {
        return getProcessEngineConfiguration().getHistoryManager();
    }

    protected DeploymentEntityManager getDeploymentEntityManager() {
        return getProcessEngineConfiguration().getDeploymentEntityManager();
    }

    protected ResourceEntityManager getResourceEntityManager() {
        return getProcessEngineConfiguration().getResourceEntityManager();
    }

    protected ByteArrayEntityManager getByteArrayEntityManager() {
        return getProcessEngineConfiguration().getByteArrayEntityManager();
    }

    protected ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return getProcessEngineConfiguration().getProcessDefinitionEntityManager();
    }

    protected ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
        return getProcessEngineConfiguration().getProcessDefinitionInfoEntityManager();
    }

    protected ModelEntityManager getModelEntityManager() {
        return getProcessEngineConfiguration().getModelEntityManager();
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return getProcessEngineConfiguration().getExecutionEntityManager();
    }

    protected EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
        return getProcessEngineConfiguration().getEventSubscriptionEntityManager();
    }

    protected HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
        return getProcessEngineConfiguration().getHistoricProcessInstanceEntityManager();
    }

    protected HistoricDetailEntityManager getHistoricDetailEntityManager() {
        return getProcessEngineConfiguration().getHistoricDetailEntityManager();
    }

    protected HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
        return getProcessEngineConfiguration().getHistoricActivityInstanceEntityManager();
    }

    protected AttachmentEntityManager getAttachmentEntityManager() {
        return getProcessEngineConfiguration().getAttachmentEntityManager();
    }

    protected CommentEntityManager getCommentEntityManager() {
        return getProcessEngineConfiguration().getCommentEntityManager();
    }
}
