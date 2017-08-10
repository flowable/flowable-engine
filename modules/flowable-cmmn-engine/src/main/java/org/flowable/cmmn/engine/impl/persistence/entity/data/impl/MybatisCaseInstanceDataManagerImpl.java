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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.AbstractCmmnDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.runtime.CaseInstance;

/**
 * @author Joram Barrez
 */
public class MybatisCaseInstanceDataManagerImpl extends AbstractCmmnDataManager<CaseInstanceEntity> implements CaseInstanceDataManager {

    public MybatisCaseInstanceDataManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public Class<? extends CaseInstanceEntity> getManagedEntityClass() {
        return CaseInstanceEntityImpl.class;
    }

    @Override
    public CaseInstanceEntity create() {
        return new CaseInstanceEntityImpl();
    }

    @Override
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

}
