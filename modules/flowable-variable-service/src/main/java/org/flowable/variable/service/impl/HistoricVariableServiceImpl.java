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
package org.flowable.variable.service.impl;

import java.util.List;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.HistoricVariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricVariableServiceImpl extends CommonServiceImpl<VariableServiceConfiguration> implements HistoricVariableService {

    public HistoricVariableServiceImpl(VariableServiceConfiguration variableServiceConfiguration) {
        super(variableServiceConfiguration);
    }
    
    @Override
    public HistoricVariableInstanceEntity getHistoricVariableInstance(String id) {
        return getHistoricVariableInstanceEntityManager().findById(id);
    }
    
    @Override
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl query) {
        return getHistoricVariableInstanceEntityManager().findHistoricVariableInstancesByQueryCriteria(query);
    }
    
    @Override
    public HistoricVariableInstanceEntity createHistoricVariableInstance() {
        return getHistoricVariableInstanceEntityManager().create();
    }
    
    @Override
    public void insertHistoricVariableInstance(HistoricVariableInstanceEntity variable) {
        getHistoricVariableInstanceEntityManager().insert(variable);
    }
    
    @Override
    public HistoricVariableInstanceEntity createAndInsert(VariableInstanceEntity variable) {
        return getHistoricVariableInstanceEntityManager().createAndInsert(variable);
    }
    
    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity) {
        HistoricVariableInstanceEntity historicVariable = getEntityCache().findInCache(HistoricVariableInstanceEntity.class, variableInstanceEntity.getId());
        HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager = getHistoricVariableInstanceEntityManager();
        if (historicVariable == null) {
            historicVariable = historicVariableInstanceEntityManager.findById(variableInstanceEntity.getId());
        }

        if (historicVariable != null) {
            historicVariableInstanceEntityManager.copyVariableValue(historicVariable, variableInstanceEntity);
        } else {
            historicVariableInstanceEntityManager.createAndInsert(variableInstanceEntity);
        }
    }
    
    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        HistoricVariableInstanceEntity historicProcessVariable = getEntityCache().findInCache(HistoricVariableInstanceEntity.class, variableInstanceEntity.getId());
        HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager = getHistoricVariableInstanceEntityManager();
        if (historicProcessVariable == null) {
            historicProcessVariable = historicVariableInstanceEntityManager.findById(variableInstanceEntity.getId());
        }

        if (historicProcessVariable != null) {
            getHistoricVariableInstanceEntityManager().delete(historicProcessVariable);
        }
    }
    
    protected EntityCache getEntityCache() {
        return Context.getCommandContext().getSession(EntityCache.class);
    }
    
    @Override
    public void deleteHistoricVariableInstance(HistoricVariableInstanceEntity historicVariable) {
        getHistoricVariableInstanceEntityManager().delete(historicVariable);
    }
    
    @Override
    public void deleteHistoricVariableInstancesByProcessInstanceId(String processInstanceId) {
        getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstanceByProcessInstanceId(processInstanceId);
    }

    @Override
    public void deleteHistoricVariableInstancesByTaskId(String taskId) {
        getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstancesByTaskId(taskId);
    }

    public HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
        return configuration.getHistoricVariableInstanceEntityManager();
    }
}
