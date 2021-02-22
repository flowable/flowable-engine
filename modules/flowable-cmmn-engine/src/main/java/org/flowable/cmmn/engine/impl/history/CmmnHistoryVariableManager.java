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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.AsyncHistorySessionCommandContextCloseListener;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryVariableManager implements InternalHistoryVariableManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public CmmnHistoryVariableManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        cmmnEngineConfiguration.getCmmnHistoryManager().recordVariableCreate(variable, createTime);
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime) {
        cmmnEngineConfiguration.getCmmnHistoryManager().recordVariableUpdate(variable, updateTime);
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable, Date removeTime) {
        // The remove time is not needed for the CmmnHistoryManager
        cmmnEngineConfiguration.getCmmnHistoryManager().recordVariableRemoved(variable);
    }

    @Override
    public void initAsyncHistoryCommandContextCloseListener() {
    	if (cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
    		CommandContext commandContext = CommandContextUtil.getCommandContext();
        	commandContext.addCloseListener(new AsyncHistorySessionCommandContextCloseListener(
        			commandContext.getSession(AsyncHistorySession.class), cmmnEngineConfiguration.getAsyncHistoryListener()));
        }
    }
}
