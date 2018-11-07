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

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public class DefaultHistoryVariableManager implements InternalHistoryVariableManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultHistoryVariableManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        getHistoryManager().recordVariableCreate(variable);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        getHistoryManager().recordVariableUpdate(variable);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        getHistoryManager().recordVariableRemoved(variable);
        if (variable.getProcessInstanceId() != null || variable.getExecutionId() != null || variable.getTaskId() != null) {
            getHistoryManager().recordHistoricDetailVariableCreate(variable, null, false);
        }
    }
    
    protected HistoryManager getHistoryManager() {
        return processEngineConfiguration.getHistoryManager();
    }
}
