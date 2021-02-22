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

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricPlanItemInstanceDataManager;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, HistoricPlanItemInstanceEntity, HistoricPlanItemInstanceDataManager>
    implements HistoricPlanItemInstanceEntityManager {

    public HistoricPlanItemInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, HistoricPlanItemInstanceDataManager historicPlanItemInstanceDataManager) {
        super(cmmnEngineConfiguration, historicPlanItemInstanceDataManager);
    }

    @Override
    public HistoricPlanItemInstanceEntity create(PlanItemInstance planItemInstance) {
        return dataManager.create(planItemInstance);
    }

    @Override
    public HistoricPlanItemInstanceQuery createHistoricPlanItemInstanceQuery() {
        return new HistoricPlanItemInstanceQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public List<HistoricPlanItemInstance> findByCaseDefinitionId(String caseDefinitionId) {
        return dataManager.findByCaseDefinitionId(caseDefinitionId);
    }

    @Override
    public List<HistoricPlanItemInstance> findByCriteria(HistoricPlanItemInstanceQuery query) {
        return dataManager.findByCriteria((HistoricPlanItemInstanceQueryImpl) query);
    }

    @Override
    public long countByCriteria(HistoricPlanItemInstanceQuery query) {
        return dataManager.countByCriteria((HistoricPlanItemInstanceQueryImpl) query);
    }
    
    @Override
    public void deleteHistoricPlanItemInstancesForNonExistingCaseInstances() {
        dataManager.deleteHistoricPlanItemInstancesForNonExistingCaseInstances();
    }
}
