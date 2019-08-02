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
package org.flowable.engine.impl.persistence.entity.data;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface HistoricProcessInstanceDataManager extends DataManager<HistoricProcessInstanceEntity> {

    HistoricProcessInstanceEntity create(ExecutionEntity processInstanceExecutionEntity);

    List<String> findHistoricProcessInstanceIdsByProcessDefinitionId(String processDefinitionId);

    List<HistoricProcessInstance> findHistoricProcessInstancesBySuperProcessInstanceId(String superProcessInstanceId);

    long findHistoricProcessInstanceCountByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery);

    List<HistoricProcessInstance> findHistoricProcessInstancesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery);

    List<HistoricProcessInstance> findHistoricProcessInstancesAndVariablesByQueryCriteria(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery);

    List<HistoricProcessInstance> findHistoricProcessInstancesByNativeQuery(Map<String, Object> parameterMap);

    long findHistoricProcessInstanceCountByNativeQuery(Map<String, Object> parameterMap);
    
    void deleteHistoricProcessInstances(HistoricProcessInstanceQueryImpl historicProcessInstanceQuery);

}
