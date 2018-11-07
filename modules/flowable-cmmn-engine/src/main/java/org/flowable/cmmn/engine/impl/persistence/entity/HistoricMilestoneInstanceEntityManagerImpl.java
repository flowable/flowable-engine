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

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricMilestoneInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricMilestoneInstanceDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public class HistoricMilestoneInstanceEntityManagerImpl extends AbstractCmmnEntityManager<HistoricMilestoneInstanceEntity> implements HistoricMilestoneInstanceEntityManager {

    protected HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager;

    public HistoricMilestoneInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager) {
        super(cmmnEngineConfiguration);
        this.historicMilestoneInstanceDataManager = historicMilestoneInstanceDataManager;
    }

    @Override
    protected DataManager<HistoricMilestoneInstanceEntity> getDataManager() {
        return historicMilestoneInstanceDataManager;
    }
    
    @Override
    public HistoricMilestoneInstanceQuery createHistoricMilestoneInstanceQuery() {
        return new HistoricMilestoneInstanceQueryImpl(cmmnEngineConfiguration.getCommandExecutor());
    }

    @Override
    public List<HistoricMilestoneInstance> findHistoricMilestoneInstancesByQueryCriteria(HistoricMilestoneInstanceQuery query) {
        return historicMilestoneInstanceDataManager.findHistoricMilestoneInstancesByQueryCriteria((HistoricMilestoneInstanceQueryImpl) query);
    }

    @Override
    public long findHistoricMilestoneInstanceCountByQueryCriteria(HistoricMilestoneInstanceQuery query) {
        return historicMilestoneInstanceDataManager.findHistoricMilestoneInstancesCountByQueryCriteria((HistoricMilestoneInstanceQueryImpl) query);
    }
    
    public HistoricMilestoneInstanceDataManager getHistoricMilestoneInstanceDataManager() {
        return historicMilestoneInstanceDataManager;
    }

    public void setHistoricMilestoneInstanceDataManager(HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager) {
        this.historicMilestoneInstanceDataManager = historicMilestoneInstanceDataManager;
    }

}
