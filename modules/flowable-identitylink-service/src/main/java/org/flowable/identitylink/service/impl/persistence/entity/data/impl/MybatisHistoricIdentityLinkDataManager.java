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
package org.flowable.identitylink.service.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.engine.common.impl.db.AbstractDataManager;
import org.flowable.engine.common.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher.HistoricIdentityLinksByProcInstMatcher;

/**
 * @author Joram Barrez
 */
public class MybatisHistoricIdentityLinkDataManager extends AbstractDataManager<HistoricIdentityLinkEntity> implements HistoricIdentityLinkDataManager {

    protected CachedEntityMatcher<HistoricIdentityLinkEntity> historicIdentityLinksByProcInstMatcher = new HistoricIdentityLinksByProcInstMatcher();

    @Override
    public Class<? extends HistoricIdentityLinkEntity> getManagedEntityClass() {
        return HistoricIdentityLinkEntityImpl.class;
    }

    @Override
    public HistoricIdentityLinkEntity create() {
        return new HistoricIdentityLinkEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectHistoricIdentityLinksByTask", taskId);
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(final String processInstanceId) {
        return getList("selectHistoricIdentityLinksByProcessInstance", processInstanceId, historicIdentityLinksByProcInstMatcher, true);
    }

}
