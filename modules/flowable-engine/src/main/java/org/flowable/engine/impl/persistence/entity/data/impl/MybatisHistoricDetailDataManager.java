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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.impl.HistoricDetailQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailAssignmentEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailAssignmentEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricDetailDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.HistoricDetailsByProcessInstanceIdEntityMatcher;
import org.flowable.engine.impl.persistence.entity.data.impl.cachematcher.HistoricDetailsByTaskInstanceIdEntityMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricDetailDataManager extends AbstractProcessDataManager<HistoricDetailEntity> implements HistoricDetailDataManager {

    private static final List<Class<? extends HistoricDetailEntity>> ENTITY_SUBCLASSES = Arrays.asList(
            HistoricDetailVariableInstanceUpdateEntityImpl.class,
            HistoricFormPropertyEntityImpl.class,
            HistoricDetailAssignmentEntityImpl.class
    );

    protected CachedEntityMatcher<HistoricDetailEntity> historicDetailsByProcessInstanceIdEntityMatcher
            = new HistoricDetailsByProcessInstanceIdEntityMatcher();

    protected CachedEntityMatcher<HistoricDetailEntity> historicDetailsByTaskIdEntityMatcher
            = new HistoricDetailsByTaskInstanceIdEntityMatcher();

    public MybatisHistoricDetailDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends HistoricDetailEntity> getManagedEntityClass() {
        return HistoricDetailEntityImpl.class;
    }

    @Override
    public List<Class<? extends HistoricDetailEntity>> getManagedEntitySubClasses() {
        return ENTITY_SUBCLASSES;
    }

    @Override
    public HistoricDetailEntity create() {
        // Superclass is abstract
        throw new UnsupportedOperationException();
    }

    @Override
    public HistoricDetailAssignmentEntity createHistoricDetailAssignment() {
        return new HistoricDetailAssignmentEntityImpl();
    }

    @Override
    public HistoricDetailVariableInstanceUpdateEntity createHistoricDetailVariableInstanceUpdate() {
        return new HistoricDetailVariableInstanceUpdateEntityImpl();
    }

    @Override
    public HistoricFormPropertyEntity createHistoricFormProperty() {
        return new HistoricFormPropertyEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricDetailEntity> findHistoricDetailsByProcessInstanceId(String processInstanceId) {
        return getList("selectHistoricDetailByProcessInstanceId", processInstanceId, historicDetailsByProcessInstanceIdEntityMatcher);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricDetailEntity> findHistoricDetailsByTaskId(String taskId) {
        return getList("selectHistoricDetailByTaskId", taskId, historicDetailsByTaskIdEntityMatcher);
    }

    @Override
    public long findHistoricDetailCountByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return (Long) getDbSqlSession().selectOne("selectHistoricDetailCountByQueryCriteria", historicVariableUpdateQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricDetail> findHistoricDetailsByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return getDbSqlSession().selectList("selectHistoricDetailsByQueryCriteria", historicVariableUpdateQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricDetail> findHistoricDetailsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectHistoricDetailByNativeQuery", parameterMap);
    }

    @Override
    public long findHistoricDetailCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectHistoricDetailCountByNativeQuery", parameterMap);
    }

    @Override
    public void bulkDeleteHistoricDetailsByProcessInstanceIds(Collection<String> historicProcessInstanceIds) {
        getDbSqlSession().delete("bulkDeleteBytesForHistoricDetailForProcessInstanceIds", createSafeInValuesList(historicProcessInstanceIds), HistoricDetailEntity.class);
        getDbSqlSession().delete("bulkDeleteHistoricDetailForProcessInstanceIds", createSafeInValuesList(historicProcessInstanceIds), HistoricDetailEntity.class);
    }
    
    @Override
    public void bulkDeleteHistoricDetailsByTaskIds(Collection<String> taskIds) {
        getDbSqlSession().delete("bulkDeleteBytesForHistoricDetailForTaskIds", createSafeInValuesList(taskIds), HistoricDetailEntity.class);
        getDbSqlSession().delete("bulkDeleteHistoricDetailForTaskIds", createSafeInValuesList(taskIds), HistoricDetailEntity.class);
    }

    @Override
    public void deleteHistoricDetailForNonExistingProcessInstances() {
        // Using HistoricDetailEntity as the entity, because the deletion order of the ByteArrayEntity is after the HistoricDetailEntity
        getDbSqlSession().delete("bulkDeleteBytesForHistoricDetailForNonExistingProcessInstances", null, HistoricDetailEntity.class);
        getDbSqlSession().delete("bulkDeleteHistoricDetailForNonExistingProcessInstances", null, HistoricDetailEntity.class);
    }
}
