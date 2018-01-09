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

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;

/**
 * @author Joram Barrez
 */
public class MybatisPlanItemInstanceDataManagerImpl extends AbstractCmmnDataManager<PlanItemInstanceEntity> implements PlanItemInstanceDataManager {
    
    public MybatisPlanItemInstanceDataManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends PlanItemInstanceEntity> getManagedEntityClass() {
        return PlanItemInstanceEntityImpl.class;
    }

    @Override
    public PlanItemInstanceEntity create() {
        PlanItemInstanceEntityImpl planItemInstanceEntityImpl = new PlanItemInstanceEntityImpl();
        
        // Avoid queries being done for new instance
        planItemInstanceEntityImpl.setChildPlanItemInstances(new ArrayList<PlanItemInstanceEntity>(1));
        planItemInstanceEntityImpl.setSatisfiedSentryPartInstances(new ArrayList<SentryPartInstanceEntity>(1));
        
        return planItemInstanceEntityImpl;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<PlanItemInstanceEntity> findChildPlanItemInstancesForCaseInstance(String caseInstanceId) {
        return getDbSqlSession().selectList("selectChildPlanItemInstancesByCaseInstanceId", caseInstanceId);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<PlanItemInstanceEntity> findChildPlanItemInstancesForStage(String stagePlanItemInstanceId) {
        return getDbSqlSession().selectList("selectChildPlanItemInstancesByStagePlanItemInstanceId", stagePlanItemInstanceId);
    }
    
    @Override
    public long countByCriteria(PlanItemInstanceQueryImpl planItemInstanceQuery) {
        return (Long) getDbSqlSession().selectOne("selectPlanItemInstanceCountByQueryCriteria", planItemInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PlanItemInstance> findByCriteria(PlanItemInstanceQueryImpl planItemInstanceQuery) {
        return getDbSqlSession().selectList("selectPlanItemInstancesByQueryCriteria", planItemInstanceQuery);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deletePlanItemInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }
    
}
