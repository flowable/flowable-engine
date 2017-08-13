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

import org.flowable.variable.service.HistoricVariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricVariableServiceImpl extends ServiceImpl implements HistoricVariableService {

    public HistoricVariableServiceImpl() {

    }

    public HistoricVariableServiceImpl(VariableServiceConfiguration variableServiceConfiguration) {
        super(variableServiceConfiguration);
    }
    
    public HistoricVariableInstanceEntity getHistoricVariableInstance(String id) {
        return getHistoricVariableInstanceEntityManager().findById(id);
    }
    
    public List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl query) {
        return getHistoricVariableInstanceEntityManager().findHistoricVariableInstancesByQueryCriteria(query);
    }
    
    public HistoricVariableInstanceEntity createHistoricVariableInstance() {
        return getHistoricVariableInstanceEntityManager().create();
    }
    
    public void insertHistoricVariableInstance(HistoricVariableInstanceEntity variable) {
        getHistoricVariableInstanceEntityManager().insert(variable);
    }
    
    public HistoricVariableInstanceEntity copyAndInsert(VariableInstanceEntity variable) {
        return getHistoricVariableInstanceEntityManager().copyAndInsert(variable);
    }
    
    public void copyVariableValue(HistoricVariableInstanceEntity historicVariable, VariableInstanceEntity variable) {
        getHistoricVariableInstanceEntityManager().copyVariableValue(historicVariable, variable);
    }
    
    public void deleteHistoricVariableInstance(String id) {
        getHistoricVariableInstanceEntityManager().delete(id);
    }
    
    public void deleteHistoricVariableInstance(HistoricVariableInstanceEntity historicVariable) {
        getHistoricVariableInstanceEntityManager().delete(historicVariable);
    }
    
    public void deleteHistoricVariableInstancesByProcessInstanceId(String processInstanceId) {
        getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstanceByProcessInstanceId(processInstanceId);
    }

    public void deleteHistoricVariableInstancesByTaskId(String taskId) {
        getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstancesByTaskId(taskId);
    }
}
