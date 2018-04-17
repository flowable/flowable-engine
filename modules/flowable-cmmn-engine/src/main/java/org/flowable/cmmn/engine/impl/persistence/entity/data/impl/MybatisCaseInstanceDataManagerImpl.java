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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.matcher.CaseInstanceByCaseDefinitionIdMatcher;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
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
        caseInstanceEntityImpl.setChildPlanItemInstances(new ArrayList<PlanItemInstanceEntity>(1));
        caseInstanceEntityImpl.setSatisfiedSentryPartInstances(new ArrayList<SentryPartInstanceEntity>(1));
        caseInstanceEntityImpl.internalSetVariableInstances(new HashMap<String, VariableInstanceEntity>(1));
        return caseInstanceEntityImpl;
    }

    @Override
    public CaseInstanceEntity findById(String caseInstanceId) {
        return findCaseInstanceEntityEagerFetchPlanItemInstances(caseInstanceId, null);
    }

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

        // The case instance will be fetched and will have all plan item instances in the childPlanItemInstances property.
        // Those children need to be properly moved to the correct parent
        CaseInstanceEntityImpl caseInstanceEntity = (CaseInstanceEntityImpl) getDbSqlSession().selectOne("selectCaseInstanceEagerFetchPlanItemInstances", params);

        if (caseInstanceEntity != null) {
            List<PlanItemInstanceEntity> allPlanItemInstances = caseInstanceEntity.getChildPlanItemInstances();
            ArrayList<PlanItemInstanceEntity> directPlanItemInstances = new ArrayList<>();
            HashMap<String, PlanItemInstanceEntity> planItemInstanceMap = new HashMap<>(allPlanItemInstances.size());

            // Map all plan item instances to its id
            for (PlanItemInstanceEntity planItemInstanceEntity : allPlanItemInstances) {

                // Mapping
                planItemInstanceMap.put(planItemInstanceEntity.getId(), planItemInstanceEntity);

                // Cache
                entityCache.put(planItemInstanceEntity, true);

                // plan items of case plan model
                if (planItemInstanceEntity.getStageInstanceId() == null) {
                    directPlanItemInstances.add(planItemInstanceEntity);
                }

                // Always add empty list, so no check is needed later and plan items
                // without children have a non-null value, not triggering the fetch
                planItemInstanceEntity.setChildPlanItemInstances(new ArrayList<PlanItemInstanceEntity>());
            }

            // Add to correct parent
            if (directPlanItemInstances.size() != planItemInstanceMap.size()) {
                for (PlanItemInstanceEntity planItemInstanceEntity : allPlanItemInstances) {
                    if (planItemInstanceEntity.getStageInstanceId() != null) {
                        PlanItemInstanceEntity parentPlanItemInstanceEntity = planItemInstanceMap.get(planItemInstanceEntity.getStageInstanceId());
                        parentPlanItemInstanceEntity.getChildPlanItemInstances().add(planItemInstanceEntity);
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
        return getDbSqlSession().selectListNoCacheCheck("selectCaseInstancesByQueryCriteria", query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CaseInstance> findWithVariablesByCriteria(CaseInstanceQueryImpl query) {
        // paging doesn't work for combining case instances and variables due
        // to an outer join, so doing it in-memory

        CaseInstanceQueryImpl caseInstanceQuery = (CaseInstanceQueryImpl) query;
        int firstResult = caseInstanceQuery.getFirstResult();
        int maxResults = caseInstanceQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (caseInstanceQuery.getCaseInstanceVariablesLimit() != null) {
            caseInstanceQuery.setMaxResults(caseInstanceQuery.getCaseInstanceVariablesLimit());
        } else {
            caseInstanceQuery.setMaxResults(cmmnEngineConfiguration.getCaseQueryLimit());
        }
        caseInstanceQuery.setFirstResult(0);

        List<CaseInstance> instanceList = getDbSqlSession().selectListWithRawParameterNoCacheCheck("selectCaseInstanceWithVariablesByQueryCriteria", caseInstanceQuery);

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = maxResults > 0 ? Math.min(maxResults, instanceList.size()) : instanceList.size();
                return instanceList.subList(0, toIndex);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public long countByCriteria(CaseInstanceQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectCaseInstanceCountByQueryCriteria", query);
    }

    @Override
    public void updateLockTime(String caseInstanceId, Date lockDate, Date expirationTime) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", caseInstanceId);
        params.put("lockTime", lockDate);
        params.put("expirationTime", expirationTime);

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

}
