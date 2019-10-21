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
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;

/**
 * @author Joram Barrez
 */
public class HistoricMilestoneInstanceEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, HistoricMilestoneInstanceEntity, HistoricMilestoneInstanceDataManager>
    implements HistoricMilestoneInstanceEntityManager {

    public HistoricMilestoneInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager) {
        super(cmmnEngineConfiguration, historicMilestoneInstanceDataManager);
    }
    
    @Override
    public HistoricMilestoneInstanceQuery createHistoricMilestoneInstanceQuery() {
        return new HistoricMilestoneInstanceQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public List<HistoricMilestoneInstance> findHistoricMilestoneInstancesByQueryCriteria(HistoricMilestoneInstanceQuery query) {
        return dataManager.findHistoricMilestoneInstancesByQueryCriteria((HistoricMilestoneInstanceQueryImpl) query);
    }

    @Override
    public long findHistoricMilestoneInstanceCountByQueryCriteria(HistoricMilestoneInstanceQuery query) {
        return dataManager.findHistoricMilestoneInstancesCountByQueryCriteria((HistoricMilestoneInstanceQueryImpl) query);
    }
    
    @Override
    public void deleteHistoricMilestoneInstancesForNonExistingCaseInstances() {
        dataManager.deleteHistoricMilestoneInstancesForNonExistingCaseInstances();
    }
    
}
