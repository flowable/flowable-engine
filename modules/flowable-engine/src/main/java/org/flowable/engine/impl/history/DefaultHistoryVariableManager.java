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

package org.flowable.engine.impl.history;

import java.util.Date;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.AsyncHistorySessionCommandContextCloseListener;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class DefaultHistoryVariableManager implements InternalHistoryVariableManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultHistoryVariableManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        getHistoryManager().recordVariableCreate(variable, createTime);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false, null, createTime);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime) {
        getHistoryManager().recordVariableUpdate(variable, updateTime);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false, null, updateTime);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable, Date removeTime) {
        getHistoryManager().recordVariableRemoved(variable);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false, null, removeTime);
        }
    }
    
    @Override
    public void initAsyncHistoryCommandContextCloseListener() {
    	if (processEngineConfiguration.isAsyncHistoryEnabled()) {
    		CommandContext commandContext = CommandContextUtil.getCommandContext();
        	commandContext.addCloseListener(new AsyncHistorySessionCommandContextCloseListener(
        			commandContext.getSession(AsyncHistorySession.class), processEngineConfiguration.getAsyncHistoryListener()));
        }
    }
    
    protected HistoryManager getHistoryManager() {
        return processEngineConfiguration.getHistoryManager();
    }
}
