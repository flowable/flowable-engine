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

import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.api.runtime.MilestoneInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.MilestoneInstanceQueryImpl;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;

/**
 * @author Joram Barrez
 */
public class MilestoneInstanceEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, MilestoneInstanceEntity, MilestoneInstanceDataManager>
    implements MilestoneInstanceEntityManager {

    public MilestoneInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, MilestoneInstanceDataManager milestoneInstanceDataManager) {
        super(cmmnEngineConfiguration, milestoneInstanceDataManager);
    }

    @Override
    public MilestoneInstanceQuery createMilestoneInstanceQuery() {
        return new MilestoneInstanceQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public List<MilestoneInstance> findMilestoneInstancesByQueryCriteria(MilestoneInstanceQuery query) {
        return dataManager.findMilestoneInstancesByQueryCriteria((MilestoneInstanceQueryImpl) query);
    }

    @Override
    public long findMilestoneInstanceCountByQueryCriteria(MilestoneInstanceQuery query) {
        return dataManager.findMilestoneInstancesCountByQueryCriteria((MilestoneInstanceQueryImpl) query);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        dataManager.deleteByCaseDefinitionId(caseDefinitionId);
    }
    
    @Override
    public void deleteByCaseInstanceId(String caseInstanceId) {
        dataManager.deleteByCaseInstanceId(caseInstanceId);
    }
    
}
