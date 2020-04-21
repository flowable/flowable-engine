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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;

/**
 * @author Joram Barrez
 */
public class MybatisPlanItemInstanceDataManagerImpl extends AbstractCmmnDataManager<PlanItemInstanceEntity> implements PlanItemInstanceDataManager {
    
    protected PlanItemInstanceByCaseInstanceIdCachedEntityMatcher planItemInstanceByCaseInstanceIdCachedEntityMatcher =
            new PlanItemInstanceByCaseInstanceIdCachedEntityMatcher();

    protected PlanItemInstanceByCaseInstanceIdAndPlanItemIdCachedEntityMatcher planItemInstanceByCaseInstanceIdAndPlanItemIdCachedEntityMatcher =
        new PlanItemInstanceByCaseInstanceIdAndPlanItemIdCachedEntityMatcher();

    protected PlanItemInstanceByStagePlanItemInstanceIdCachedEntityMatcher planItemInstanceByStagePlanItemInstanceIdCachedEntityMatcher =
        new PlanItemInstanceByStagePlanItemInstanceIdCachedEntityMatcher();
    
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
        planItemInstanceEntityImpl.setChildPlanItemInstances(new ArrayList<>(1));
        planItemInstanceEntityImpl.setSatisfiedSentryPartInstances(new ArrayList<>(1));
        planItemInstanceEntityImpl.internalSetVariableInstances(new HashMap<>());
        
        return planItemInstanceEntityImpl;
    }
    
    @Override
    public PlanItemInstanceEntity findById(String planItemInstanceId) {
        // Could have been cached before
        EntityCache entityCache = getEntityCache();
        PlanItemInstanceEntity cachedPlanItemInstanceEntity = entityCache.findInCache(getManagedEntityClass(), planItemInstanceId);
        if (cachedPlanItemInstanceEntity != null) {
            return cachedPlanItemInstanceEntity;
        }
        
        cmmnEngineConfiguration.getCaseInstanceDataManager().findCaseInstanceEntityEagerFetchPlanItemInstances(null, planItemInstanceId);
        
        // the plan item instance will be in the cache now due to fetching the case instance,
        // no need to do anything extra, the findById of the super class will look into the cache
        return super.findById(planItemInstanceId);
    }
    
    @Override
    public List<PlanItemInstanceEntity> findByCaseInstanceId(String caseInstanceId) {
        return getList("selectPlanItemInstancesByCaseInstanceId", caseInstanceId, planItemInstanceByCaseInstanceIdCachedEntityMatcher, true);
    }

    @Override
    public List<PlanItemInstanceEntity> findByStagePlanItemInstanceId(String stagePlanItemInstanceId) {
        return getList("selectPlanItemInstancesByStagePlanItemInstanceId", stagePlanItemInstanceId, planItemInstanceByStagePlanItemInstanceIdCachedEntityMatcher, true);
    }

    @Override
    public List<PlanItemInstanceEntity> findByCaseInstanceIdAndPlanItemId(String caseInstanceId, String planitemId) {
        Map<String, Object> params = new HashMap<>();
        params.put("caseInstanceId", caseInstanceId);
        params.put("planItemId", planitemId);
        return getList("selectPlanItemInstancesByCaseInstanceIdAndPlanItemId", params, planItemInstanceByCaseInstanceIdAndPlanItemIdCachedEntityMatcher);
    }

    @Override
    public long countByCriteria(PlanItemInstanceQueryImpl planItemInstanceQuery) {
        return (Long) getDbSqlSession().selectOne("selectPlanItemInstanceCountByQueryCriteria", planItemInstanceQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PlanItemInstance> findByCriteria(PlanItemInstanceQueryImpl planItemInstanceQuery) {
        return getDbSqlSession().selectList("selectPlanItemInstancesByQueryCriteria", planItemInstanceQuery, getManagedEntityClass());
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deletePlanItemInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
    }
    
    @Override
    public void deleteByStageInstanceId(String stageInstanceId) {
        List<PlanItemInstanceEntityImpl> planItemInstanceEntities = getEntityCache().findInCache(PlanItemInstanceEntityImpl.class);
        for (PlanItemInstanceEntityImpl planItemInstanceEntity : planItemInstanceEntities) {
            if (stageInstanceId.equals(planItemInstanceEntity.getStageInstanceId())) {
                getDbSqlSession().delete(planItemInstanceEntity);
            }
        }
        getDbSqlSession().delete("deletePlanItemInstancesByStageInstanceId", stageInstanceId, getManagedEntityClass());
    }
    
    @Override
    public void deleteByCaseInstanceId(String caseInstanceId) {
        bulkDelete("deletePlanItemInstancesByCaseInstanceId", planItemInstanceByCaseInstanceIdCachedEntityMatcher, caseInstanceId);
    }
    
    public static class PlanItemInstanceByCaseInstanceIdCachedEntityMatcher extends CachedEntityMatcherAdapter<PlanItemInstanceEntity> {

        @Override
        public boolean isRetained(PlanItemInstanceEntity entity, Object param) {
            String caseInstanceId = (String) param;
            return caseInstanceId.equals(entity.getCaseInstanceId());
        }
        
    }

    public static class PlanItemInstanceByCaseInstanceIdAndPlanItemIdCachedEntityMatcher extends CachedEntityMatcherAdapter<PlanItemInstanceEntity> {

        @Override
        public boolean isRetained(PlanItemInstanceEntity entity, Object param) {
            Map<String, Object> map = (Map<String, Object>) param;
            String caseInstanceId = (String) map.get("caseInstanceId");
            String planItemId = (String) map.get("planItemId");
            return caseInstanceId.equals(entity.getCaseInstanceId()) && planItemId.equals(entity.getPlanItem().getId());
        }

    }

    public static class PlanItemInstanceByStagePlanItemInstanceIdCachedEntityMatcher extends CachedEntityMatcherAdapter<PlanItemInstanceEntity> {

        @Override
        public boolean isRetained(PlanItemInstanceEntity entity, Object param) {
            String stagePlanItemInstanceId = (String) param;
            return stagePlanItemInstanceId.equals(entity.getStageInstanceId());
        }

    }
    
}
