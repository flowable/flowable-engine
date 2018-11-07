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
package org.flowable.cmmn.engine.impl.persistence.entity.data;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public interface HistoricCaseInstanceDataManager extends DataManager<HistoricCaseInstanceEntity> {
    
    List<HistoricCaseInstanceEntity> findHistoricCaseInstancesByCaseDefinitionId(String caseDefinitionId);
    
    List<HistoricCaseInstance> findByCriteria(HistoricCaseInstanceQueryImpl query);
    
    long countByCriteria(HistoricCaseInstanceQueryImpl query);

    List<HistoricCaseInstance> findWithVariablesByQueryCriteria(HistoricCaseInstanceQueryImpl historicCaseInstanceQuery);

    void deleteByCaseDefinitionId(String caseDefinitionId);
    
}
