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

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public interface HistoricVariableInstanceDataManager extends DataManager<HistoricVariableInstanceEntity> {

    List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByProcessInstanceId(String processInstanceId);

    List<HistoricVariableInstanceEntity> findHistoricVariableInstancesByTaskId(String taskId);

    long findHistoricVariableInstanceCountByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery);

    List<HistoricVariableInstance> findHistoricVariableInstancesByQueryCriteria(HistoricVariableInstanceQueryImpl historicProcessVariableQuery, Page page);

    HistoricVariableInstanceEntity findHistoricVariableInstanceByVariableInstanceId(String variableInstanceId);

    List<HistoricVariableInstance> findHistoricVariableInstancesByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults);

    long findHistoricVariableInstanceCountByNativeQuery(Map<String, Object> parameterMap);

}
