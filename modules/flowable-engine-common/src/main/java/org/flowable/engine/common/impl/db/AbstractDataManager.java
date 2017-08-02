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
package org.flowable.engine.common.impl.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.common.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class AbstractDataManager<EntityImpl extends Entity> implements DataManager<EntityImpl> {

    public abstract Class<? extends EntityImpl> getManagedEntityClass();

    public List<Class<? extends EntityImpl>> getManagedEntitySubClasses() {
        return null;
    }
    
    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    protected DbSqlSession getDbSqlSession() {
        return getSession(DbSqlSession.class);
    }
    
    protected EntityCache getEntityCache() {
        return getSession(EntityCache.class);
    }

    @Override
    public EntityImpl findById(String entityId) {
        if (entityId == null) {
            return null;
        }

        // Cache
        EntityImpl cachedEntity = getEntityCache().findInCache(getManagedEntityClass(), entityId);
        if (cachedEntity != null) {
            return cachedEntity;
        }

        // Database
        return getDbSqlSession().selectById(getManagedEntityClass(), entityId, false);
    }

    @Override
    public void insert(EntityImpl entity) {
        getDbSqlSession().insert(entity);
    }

    public EntityImpl update(EntityImpl entity) {
        getDbSqlSession().update(entity);
        return entity;
    }

    @Override
    public void delete(String id) {
        EntityImpl entity = findById(id);
        delete(entity);
    }

    @Override
    public void delete(EntityImpl entity) {
        getDbSqlSession().delete(entity);
    }

    @SuppressWarnings("unchecked")
    protected EntityImpl findByQuery(String selectQuery, Object parameter) {
        return (EntityImpl) getDbSqlSession().selectOne(selectQuery, parameter);
    }

    @SuppressWarnings("unchecked")
    protected List<EntityImpl> getList(String dbQueryName, Object parameter) {
        Collection<EntityImpl> result = getDbSqlSession().selectList(dbQueryName, parameter);
        return new ArrayList<>(result);
    }

}
