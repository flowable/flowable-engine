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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.matcher.CaseInstanceByCaseDefinitionIdMatcher;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class MybatisCaseInstanceDataManagerImpl extends AbstractCmmnDataManager<CaseInstanceEntity> implements CaseInstanceDataManager {

    protected CaseInstanceByCaseDefinitionIdMatcher caseInstanceByCaseDefinitionIdMatcher = new CaseInstanceByCaseDefinitionIdMatcher();

    public MybatisCaseInstanceDataManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends CaseInstanceEntity> getManagedEntityClass() {
        return CaseInstanceEntityImpl.class;
    }

    @Override
    public CaseInstanceEntity create() {
        CaseInstanceEntityImpl caseInstanceEntityImpl = new CaseInstanceEntityImpl();
        caseInstanceEntityImpl.setChildPlanItemInstances(new ArrayList<>(1));
        caseInstanceEntityImpl.setSatisfiedSentryPartInstances(new ArrayList<>(1));
        caseInstanceEntityImpl.internalSetVariableInstances(new HashMap<>(1));
        return caseInstanceEntityImpl;
    }

    @Override
    public CaseInstanceEntity create(HistoricCaseInstance historicCaseInstance, Map<String, VariableInstanceEntity> variables) {
        return new CaseInstanceEntityImpl(historicCaseInstance, variables);
    }

    @Override
    public CaseInstanceEntity findById(String caseInstanceId) {
        return findCaseInstanceEntityEagerFetchPlanItemInstances(caseInstanceId, null);
    }

    @Override
    public CaseInstanceEntity findCaseInstanceEntityEagerFetchPlanItemInstances(String caseInstanceId, String planItemInstanceId) {

        // Could have been fetched before
        EntityCache entityCache = getEntityCache();
        CaseInstanceEntity cachedCaseInstanceEntity = entityCache.findInCache(getManagedEntityClass(), caseInstanceId);
        if (cachedCaseInstanceEntity != null) {
            return cachedCaseInstanceEntity;
        }

        // Not in cache
        HashMap<String, Object> params = new HashMap<>(1);
        if (caseInstanceId != null) {
            params.put("caseInstanceId", caseInstanceId);
        } else if (planItemInstanceId != null) {
            params.put("planItemInstanceId", planItemInstanceId);
        }

        if (params.isEmpty()) {
            throw new FlowableIllegalArgumentException("selectCaseInstanceEagerFetchPlanItemInstances needs either caseInstanceId or planItemInstanceId");
        }

        // The case instance will be fetched and will have all plan item instances in the childPlanItemInstances property.
        // Those children need to be properly moved to the correct parent
        CaseInstanceEntityImpl caseInstanceEntity = (CaseInstanceEntityImpl) getDbSqlSession().selectOne("selectCaseInstanceEagerFetchPlanItemInstances", params);

        if (caseInstanceEntity != null) {
            List<PlanItemInstanceEntity> allPlanItemInstances = caseInstanceEntity.getChildPlanItemInstances();
            ArrayList<PlanItemInstanceEntity> directPlanItemInstances = new ArrayList<>();
            HashMap<String, PlanItemInstanceEntity> planItemInstanceMap = new HashMap<>(allPlanItemInstances.size());

            // Map all plan item instances to its id
            for (PlanItemInstanceEntity planItemInstanceEntity : allPlanItemInstances) {

                PlanItemInstanceEntity currentPlanItemInstanceEntity = planItemInstanceEntity;

                // If it's already in the cache, it has precedence on the fetched one
                PlanItemInstanceEntity planItemInstanceFromCache = entityCache.findInCache(PlanItemInstanceEntityImpl.class, planItemInstanceEntity.getId());
                if (planItemInstanceFromCache != null) {
                    // Mapping
                    planItemInstanceMap.put(planItemInstanceFromCache.getId(), planItemInstanceFromCache);

                    currentPlanItemInstanceEntity = planItemInstanceFromCache;

                } else {
                    // Mapping
                    planItemInstanceMap.put(planItemInstanceEntity.getId(), planItemInstanceEntity);

                    // Cache
                    entityCache.put(planItemInstanceEntity, true);

                    // Always add empty list, so no check is needed later and plan items
                    // without children have a non-null value, not triggering the fetch
                    currentPlanItemInstanceEntity.setChildPlanItemInstances(new ArrayList<>());
                }

                // plan items of case plan model
                if (currentPlanItemInstanceEntity.getStageInstanceId() == null) {
                    directPlanItemInstances.add(currentPlanItemInstanceEntity);
                }

            }

            // Add to correct parent
            if (directPlanItemInstances.size() != planItemInstanceMap.size()) {
                for (PlanItemInstanceEntity planItemInstanceEntity : allPlanItemInstances) {
                    if (planItemInstanceEntity.getStageInstanceId() != null) {
                        PlanItemInstanceEntity parentPlanItemInstanceEntity = planItemInstanceMap.get(planItemInstanceEntity.getStageInstanceId());

                        // It can happen the parent plan item instance does not exist:
                        // For example when a nested B is nested in a stage A and both are repeating.
                        // The wait_for_repetition of B has the old stage A plan item instance as parent,
                        // and it won't be returned by the eager fetch query
                        if (parentPlanItemInstanceEntity != null) {
                            parentPlanItemInstanceEntity.getChildPlanItemInstances().add(planItemInstanceMap.get(planItemInstanceEntity.getId()));
                        }
                    }
                }
            }

            caseInstanceEntity.setChildPlanItemInstances(directPlanItemInstances);
            return caseInstanceEntity;

        } else {
            return null;

        }
    }

    @Override
    public List<CaseInstanceEntity> findCaseInstancesByCaseDefinitionId(String caseDefinitionId) {
        return getList("selectCaseInstancesByCaseDefinitionId", caseDefinitionId, caseInstanceByCaseDefinitionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CaseInstance> findByCriteria(CaseInstanceQueryImpl query) {
        // Not going through cache as the case instance should always be loaded with all related plan item instances
        // when not doing a query call
        setSafeInValueLists(query);
        return getDbSqlSession().selectListNoCacheLoadAndStore("selectCaseInstancesByQueryCriteria", query, getManagedEntityClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CaseInstance> findWithVariablesByCriteria(CaseInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return getDbSqlSession().selectListNoCacheLoadAndStore("selectCaseInstanceWithVariablesByQueryCriteria", query, getManagedEntityClass());
    }

    @Override
    public long countByCriteria(CaseInstanceQueryImpl query) {
        setSafeInValueLists(query);
        return (Long) getDbSqlSession().selectOne("selectCaseInstanceCountByQueryCriteria", query);
    }

    @Override
    public void updateLockTime(String caseInstanceId, Date lockDate, String lockOwner, Date expirationTime) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", caseInstanceId);
        params.put("lockTime", lockDate);
        params.put("expirationTime", expirationTime);
        params.put("lockOwner", lockOwner);

        int result = getDbSqlSession().update("updateCaseInstanceLockTime", params);
        if (result == 0) {
            throw new FlowableOptimisticLockingException("Could not lock case instance");
        }
    }

    @Override
    public void clearLockTime(String caseInstanceId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", caseInstanceId);
        getDbSqlSession().update("clearCaseInstanceLockTime", params);
    }

    @Override
    public void clearAllLockTimes(String lockOwner) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("lockOwner", lockOwner);
        getDbSqlSession().update("clearAllCaseInstanceLockTimes", params);
    }

    protected void setSafeInValueLists(CaseInstanceQueryImpl caseInstanceQuery) {
        if (caseInstanceQuery.getInvolvedGroups() != null) {
            caseInstanceQuery.setSafeInvolvedGroups(createSafeInValuesList(caseInstanceQuery.getInvolvedGroups()));
        }
        
        if (caseInstanceQuery.getOrQueryObjects() != null && !caseInstanceQuery.getOrQueryObjects().isEmpty()) {
            for (CaseInstanceQueryImpl orCaseInstanceQuery : caseInstanceQuery.getOrQueryObjects()) {
                setSafeInValueLists(orCaseInstanceQuery);
            }
        }
    }
}
