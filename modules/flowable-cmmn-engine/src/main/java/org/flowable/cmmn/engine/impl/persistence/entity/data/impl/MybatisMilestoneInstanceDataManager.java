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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.MilestoneInstanceQueryImpl;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;

/**
 * @author Joram Barrez
 */
public class MybatisMilestoneInstanceDataManager extends AbstractCmmnDataManager<MilestoneInstanceEntity> implements MilestoneInstanceDataManager {
    
    protected MilestoneInstanceByCaseInstanceIdCachedEntityMatcher milestoneInstanceByCaseInstanceIdCachedEntityMatcher 
        = new MilestoneInstanceByCaseInstanceIdCachedEntityMatcher();

    public MybatisMilestoneInstanceDataManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends MilestoneInstanceEntity> getManagedEntityClass() {
        return MilestoneInstanceEntityImpl.class;
    }

    @Override
    public MilestoneInstanceEntity create() {
        return new MilestoneInstanceEntityImpl();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<MilestoneInstance> findMilestoneInstancesByQueryCriteria(MilestoneInstanceQueryImpl query) {
        return getDbSqlSession().selectList("selectMilestoneInstancesByQueryCriteria", query);
    }
    
    @Override
    public long findMilestoneInstancesCountByQueryCriteria(MilestoneInstanceQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectMilestoneInstanceCountByQueryCriteria", query);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deleteMilestoneInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }
    
    @Override
    public void deleteByCaseInstanceId(String caseInstanceId) {
        bulkDelete("deleteMilestoneInstanceByCaseInstanceId", milestoneInstanceByCaseInstanceIdCachedEntityMatcher, caseInstanceId);
    }
    
    public static class MilestoneInstanceByCaseInstanceIdCachedEntityMatcher extends CachedEntityMatcherAdapter<MilestoneInstanceEntity> {

        @Override
        public boolean isRetained(MilestoneInstanceEntity entity, Object param) {
            String caseInstanceId = (String) param;
            return caseInstanceId.equals(entity.getCaseInstanceId());
        }
        
    }

}
