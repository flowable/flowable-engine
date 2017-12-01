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
import org.flowable.engine.common.api.FlowableOptimisticLockingException;
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
    public List<CaseInstanceEntity> findCaseInstancesByCaseDefinitionId(String caseDefinitionId) {
        return getList("selectCaseInstancesByCaseDefinitionId", caseDefinitionId, caseInstanceByCaseDefinitionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CaseInstance> findByCriteria(CaseInstanceQueryImpl query) {
        return getDbSqlSession().selectList("selectCaseInstancesByQueryCriteria", query);
    }

    @Override
    public long countByCriteria(CaseInstanceQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectCaseInstanceCountByQueryCriteria", query);
    }
    
    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        getDbSqlSession().delete("deleteCaseInstanceByCaseDefinitionId", caseDefinitionId, getManagedEntityClass());
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
