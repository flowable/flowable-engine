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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricCaseInstanceDataManager;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class HistoricCaseInstanceEntityManagerImpl extends AbstractCmmnEntityManager<HistoricCaseInstanceEntity> implements HistoricCaseInstanceEntityManager {

    protected HistoricCaseInstanceDataManager historicCaseInstanceDataManager;

    public HistoricCaseInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, HistoricCaseInstanceDataManager historicCaseInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.historicCaseInstanceDataManager = historicCaseInstanceDataManager;
    }

    @Override
    protected DataManager<HistoricCaseInstanceEntity> getDataManager() {
        return historicCaseInstanceDataManager;
    }
    
    @Override
    public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
        return new HistoricCaseInstanceQueryImpl(cmmnEngineConfiguration.getCommandExecutor());
    }

    @Override
    public List<HistoricCaseInstance> findByCriteria(HistoricCaseInstanceQuery query) {
        return historicCaseInstanceDataManager.findByCriteria((HistoricCaseInstanceQueryImpl) query);
    }

    @Override
   public long countByCriteria(HistoricCaseInstanceQuery query) {
        return historicCaseInstanceDataManager.countByCriteria((HistoricCaseInstanceQueryImpl) query);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        historicCaseInstanceDataManager.deleteByCaseDefinitionId(caseDefinitionId);
    }

}
