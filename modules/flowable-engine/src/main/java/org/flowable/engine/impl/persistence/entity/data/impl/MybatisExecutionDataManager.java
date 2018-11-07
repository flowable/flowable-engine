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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.PerformanceSettings;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionByProcessInstanceMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsByParentExecutionIdAndActivityIdEntityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsByParentExecutionIdEntityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsByProcessInstanceIdEntityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsByRootProcessInstanceMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ExecutionsWithSameRootProcessInstanceIdMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.InactiveExecutionsByProcInstMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.InactiveExecutionsInActivityAndProcInstMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.InactiveExecutionsInActivityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.ProcessInstancesByProcessDefinitionMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.SubProcessInstanceExecutionBySuperExecutionIdMatcher;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 */
public class MybatisExecutionDataManager extends AbstractProcessDataManager<ExecutionEntity> implements ExecutionDataManager {

    protected PerformanceSettings performanceSettings;

    protected CachedEntityMatcher<ExecutionEntity> executionsByParentIdMatcher = new ExecutionsByParentExecutionIdEntityMatcher();

    protected CachedEntityMatcher<ExecutionEntity> executionsByProcessInstanceIdMatcher = new ExecutionsByProcessInstanceIdEntityMatcher();

    protected SingleCachedEntityMatcher<ExecutionEntity> subProcessInstanceBySuperExecutionIdMatcher = new SubProcessInstanceExecutionBySuperExecutionIdMatcher();

    protected CachedEntityMatcher<ExecutionEntity> executionsWithSameRootProcessInstanceIdMatcher = new ExecutionsWithSameRootProcessInstanceIdMatcher();

    protected CachedEntityMatcher<ExecutionEntity> inactiveExecutionsInActivityAndProcInstMatcher = new InactiveExecutionsInActivityAndProcInstMatcher();

    protected CachedEntityMatcher<ExecutionEntity> inactiveExecutionsByProcInstMatcher = new InactiveExecutionsByProcInstMatcher();

    protected CachedEntityMatcher<ExecutionEntity> inactiveExecutionsInActivityMatcher = new InactiveExecutionsInActivityMatcher();

    protected CachedEntityMatcher<ExecutionEntity> executionByProcessInstanceMatcher = new ExecutionByProcessInstanceMatcher();

    protected CachedEntityMatcher<ExecutionEntity> executionsByRootProcessInstanceMatcher = new ExecutionsByRootProcessInstanceMatcher();

    protected CachedEntityMatcher<ExecutionEntity> executionsByParentExecutionIdAndActivityIdEntityMatcher = new ExecutionsByParentExecutionIdAndActivityIdEntityMatcher();

    protected CachedEntityMatcher<ExecutionEntity> processInstancesByProcessDefinitionMatcher = new ProcessInstancesByProcessDefinitionMatcher();

    public MybatisExecutionDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
        this.performanceSettings = processEngineConfiguration.getPerformanceSettings();
    }

    @Override
    public Class<? extends ExecutionEntity> getManagedEntityClass() {
        return ExecutionEntityImpl.class;
    }

    @Override
    public ExecutionEntity create() {
        return ExecutionEntityImpl.createWithEmptyRelationshipCollections();
    }

    @Override
    public ExecutionEntity findById(String executionId) {
        if (isExecutionTreeFetched(executionId)) {
            return getEntityCache().findInCache(getManagedEntityClass(), executionId);
        }
        return super.findById(executionId);
    }
    
    /**
     * Fetches the execution tree related to the execution (if the process definition has been configured to do so)
     * @return True if the tree has been fetched, false otherwise or if fetching is disabled.  
     */
    protected boolean isExecutionTreeFetched(final String executionId) {
        
        // The setting needs to be globally enabled
        if (!performanceSettings.isEnableEagerExecutionTreeFetching()) {
            return false;
        }
        
        // Need to get the cache result before doing the findById
        ExecutionEntity cachedExecutionEntity = getEntityCache().findInCache(getManagedEntityClass(), executionId);
        
        // Find execution in db or cache to check process definition setting for execution fetch.
        // If not set, no extra work is done. The execution is in the cache however now as a side-effect of calling this method.
        ExecutionEntity executionEntity = (cachedExecutionEntity != null) ? cachedExecutionEntity : super.findById(executionId);
        if (!ProcessDefinitionUtil.getProcess(executionEntity.getProcessDefinitionId()).isEnableEagerExecutionTreeFetching()) {
            return false;
        }
        
        // If it's in the cache, the execution and its tree have been fetched before. No need to do anything more.
        if (cachedExecutionEntity != null) {
            return true;
        }
        
        // Fetches execution tree. This will store them in the cache and thus avoind extra database calls.
        getList("selectExecutionsWithSameRootProcessInstanceId", executionId,
                executionsWithSameRootProcessInstanceIdMatcher, true);
        
        return true;
    }

    @Override
    public ExecutionEntity findSubProcessInstanceBySuperExecutionId(final String superExecutionId) {
        boolean treeFetched = isExecutionTreeFetched(superExecutionId);
        return getEntity("selectSubProcessInstanceBySuperExecutionId",
                superExecutionId,
                subProcessInstanceBySuperExecutionIdMatcher,
                !treeFetched);
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByParentExecutionId(final String parentExecutionId) {
        if (isExecutionTreeFetched(parentExecutionId)) {
            return getListFromCache(executionsByParentIdMatcher, parentExecutionId);
        } else {
            return getList("selectExecutionsByParentExecutionId", parentExecutionId, executionsByParentIdMatcher, true);
        }
    }

    @Override
    public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(final String processInstanceId) {
        if (isExecutionTreeFetched(processInstanceId)) {
            return getListFromCache(executionsByProcessInstanceIdMatcher, processInstanceId);
        } else {
            return getList("selectChildExecutionsByProcessInstanceId", processInstanceId, executionsByProcessInstanceIdMatcher, true);
        }
    }

    @Override
    public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds) {
        Map<String, Object> parameters = new HashMap<>(2);
        parameters.put("parentExecutionId", parentExecutionId);
        parameters.put("activityIds", activityIds);

        if (isExecutionTreeFetched(parentExecutionId)) {
            return getListFromCache(executionsByParentExecutionIdAndActivityIdEntityMatcher, parameters);
        } else {
            return getList("selectExecutionsByParentExecutionAndActivityIds", parameters, executionsByParentExecutionIdAndActivityIdEntityMatcher, true);
        }
    }

    @Override
    public List<ExecutionEntity> findExecutionsByRootProcessInstanceId(final String rootProcessInstanceId) {
        if (isExecutionTreeFetched(rootProcessInstanceId)) {
            return getListFromCache(executionsByRootProcessInstanceMatcher, rootProcessInstanceId);
        } else {
            return getList("selectExecutionsByRootProcessInstanceId", rootProcessInstanceId, executionsByRootProcessInstanceMatcher, true);
        }
    }

    @Override
    public List<ExecutionEntity> findExecutionsByProcessInstanceId(final String processInstanceId) {
        if (isExecutionTreeFetched(processInstanceId)) {
            return getListFromCache(executionByProcessInstanceMatcher, processInstanceId);
        } else {
            return getList("selectExecutionsByProcessInstanceId", processInstanceId, executionByProcessInstanceMatcher, true);
        }
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(final String processInstanceId) {
        HashMap<String, Object> params = new HashMap<>(2);
        params.put("processInstanceId", processInstanceId);
        params.put("isActive", false);

        if (isExecutionTreeFetched(processInstanceId)) {
            return getListFromCache(inactiveExecutionsByProcInstMatcher, params);
        } else {
            return getList("selectInactiveExecutionsForProcessInstance", params, inactiveExecutionsByProcInstMatcher, true);
        }
    }

    @Override
    public Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(final String activityId, final String processInstanceId) {
        HashMap<String, Object> params = new HashMap<>(3);
        params.put("activityId", activityId);
        params.put("processInstanceId", processInstanceId);
        params.put("isActive", false);

        if (isExecutionTreeFetched(processInstanceId)) {
            return getListFromCache(inactiveExecutionsInActivityAndProcInstMatcher, params);
        } else {
            return getList("selectInactiveExecutionsInActivityAndProcessInstance", params, inactiveExecutionsInActivityAndProcInstMatcher, true);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> findProcessInstanceIdsByProcessDefinitionId(String processDefinitionId) {
        return getDbSqlSession().selectListNoCacheCheck("selectProcessInstanceIdsByProcessDefinitionId", processDefinitionId);
    }

    @Override
    public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
        return (Long) getDbSqlSession().selectOne("selectExecutionCountByQueryCriteria", executionQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery) {
        // False -> executions should not be cached if using executionTreeFetching
        boolean useCache = !performanceSettings.isEnableEagerExecutionTreeFetching();
        if (useCache) {
            return getDbSqlSession().selectList("selectExecutionsByQueryCriteria", executionQuery);
        } else {
            return getDbSqlSession().selectListNoCacheCheck("selectExecutionsByQueryCriteria", executionQuery);
        }
    }

    @Override
    public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        return (Long) getDbSqlSession().selectOne("selectProcessInstanceCountByQueryCriteria", executionQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        // False -> executions should not be cached if using executionTreeFetching
        boolean useCache = !performanceSettings.isEnableEagerExecutionTreeFetching();
        if (useCache) {
            return getDbSqlSession().selectList("selectProcessInstanceByQueryCriteria", executionQuery);
        } else {
            return getDbSqlSession().selectListNoCacheCheck("selectProcessInstanceByQueryCriteria", executionQuery);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
        // paging doesn't work for combining process instances and variables due
        // to an outer join, so doing it in-memory

        int firstResult = executionQuery.getFirstResult();
        int maxResults = executionQuery.getMaxResults();

        // setting max results, limit to 20000 results for performance reasons
        if (executionQuery.getProcessInstanceVariablesLimit() != null) {
            executionQuery.setMaxResults(executionQuery.getProcessInstanceVariablesLimit());
        } else {
            executionQuery.setMaxResults(getProcessEngineConfiguration().getExecutionQueryLimit());
        }
        executionQuery.setFirstResult(0);

        List<ProcessInstance> instanceList = getDbSqlSession().selectListWithRawParameterNoCacheCheck("selectProcessInstanceWithVariablesByQueryCriteria", executionQuery);

        if (instanceList != null && !instanceList.isEmpty()) {
            if (firstResult > 0) {
                if (firstResult <= instanceList.size()) {
                    int toIndex = firstResult + Math.min(maxResults, instanceList.size() - firstResult);
                    return instanceList.subList(firstResult, toIndex);
                } else {
                    return Collections.EMPTY_LIST;
                }
            } else {
                int toIndex = maxResults > 0 ?  Math.min(maxResults, instanceList.size()) : instanceList.size();
                return instanceList.subList(0, toIndex);
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectExecutionByNativeQuery", parameterMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectExecutionByNativeQuery", parameterMap);
    }

    @Override
    public long findExecutionCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectExecutionCountByNativeQuery", parameterMap);
    }

    @Override
    public void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().update("updateExecutionTenantIdForDeployment", params);
    }

    @Override
    public void updateProcessInstanceLockTime(String processInstanceId, Date lockDate, Date expirationTime) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", processInstanceId);
        params.put("lockTime", lockDate);
        params.put("expirationTime", expirationTime);

        int result = getDbSqlSession().update("updateProcessInstanceLockTime", params);
        if (result == 0) {
            throw new FlowableOptimisticLockingException("Could not lock process instance");
        }
    }

    @Override
    public void updateAllExecutionRelatedEntityCountFlags(boolean newValue) {
        getDbSqlSession().update("updateExecutionRelatedEntityCountEnabled", newValue);
    }

    @Override
    public void clearProcessInstanceLockTime(String processInstanceId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", processInstanceId);
        getDbSqlSession().update("clearProcessInstanceLockTime", params);
    }
    
}
