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
package org.flowable.content.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.content.api.ContentItem;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.ContentItemQueryImpl;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntity;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntityImpl;
import org.flowable.content.engine.impl.persistence.entity.data.AbstractContentDataManager;
import org.flowable.content.engine.impl.persistence.entity.data.ContentItemDataManager;

/**
 * @author Tijs Rademakers
 */
public class MybatisContentItemDataManager extends AbstractContentDataManager<ContentItemEntity> implements ContentItemDataManager {

    public MybatisContentItemDataManager(ContentEngineConfiguration contentEngineConfiguration) {
        super(contentEngineConfiguration);
    }

    @Override
    public Class<? extends ContentItemEntity> getManagedEntityClass() {
        return ContentItemEntityImpl.class;
    }

    @Override
    public ContentItemEntity create() {
        return new ContentItemEntityImpl();
    }

    @Override
    public long findContentItemCountByQueryCriteria(ContentItemQueryImpl contentItemQuery) {
        return (Long) getDbSqlSession().selectOne("selectContentItemCountByQueryCriteria", contentItemQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ContentItem> findContentItemsByQueryCriteria(ContentItemQueryImpl contentItemQuery) {
        return getDbSqlSession().selectList("selectContentItemsByQueryCriteria", contentItemQuery, getManagedEntityClass());
    }

    @Override
    public void deleteContentItemsByTaskId(String taskId) {
        getDbSqlSession().delete("deleteContentItemsByTaskId", taskId, getManagedEntityClass());
    }

    @Override
    public void deleteContentItemsByProcessInstanceId(String processInstanceId) {
        getDbSqlSession().delete("deleteContentItemsByProcessInstanceId", processInstanceId, getManagedEntityClass());
    }

    @Override
    public void deleteContentItemsByScopeIdAndScopeType(String scopeId, String scopeType) {
        Map<String, String> params = new HashMap<>(2);
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        getDbSqlSession().delete("deleteContentItemsByScopeIdAndScopeType", params, getManagedEntityClass());
    }
}
