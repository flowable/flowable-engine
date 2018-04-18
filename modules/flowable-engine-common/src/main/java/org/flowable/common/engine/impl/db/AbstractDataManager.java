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
package org.flowable.common.engine.impl.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

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

    @Override
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
    
    @SuppressWarnings("unchecked")
    protected EntityImpl getEntity(String selectQuery, Object parameter, SingleCachedEntityMatcher<EntityImpl> cachedEntityMatcher, boolean checkDatabase) {
        // Cache
        for (EntityImpl cachedEntity : getEntityCache().findInCache(getManagedEntityClass())) {
            if (cachedEntityMatcher.isRetained(cachedEntity, parameter)) {
                return cachedEntity;
            }
        }

        // Database
        if (checkDatabase) {
            return (EntityImpl) getDbSqlSession().selectOne(selectQuery, parameter);
        }

        return null;
    }
    
    protected List<EntityImpl> getList(String dbQueryName, Object parameter, CachedEntityMatcher<EntityImpl> cachedEntityMatcher) {
        return getList(dbQueryName, parameter, cachedEntityMatcher, true);
    }

    /**
     * Gets a list by querying the database and the cache using {@link CachedEntityMatcher}. First, the entities are fetched from the database using the provided query. The cache is then queried for
     * the entities of the same type. If an entity matches the {@link CachedEntityMatcher} condition, it replaces the entity from the database (as it is newer).
     * 
     * @param dbQueryName
     *            The query name that needs to be executed.
     * @param parameter
     *            The parameters for the query.
     * @param cachedEntityMatcher
     *            The matcher used to determine which entities from the cache needs to be retained
     * @param checkCache
     *            If false, no cache check will be done, and the returned list will simply be the list from the database.
     */
    protected List<EntityImpl> getList(String dbQueryName, Object parameter,
            CachedEntityMatcher<EntityImpl> cachedEntityMatcher, boolean checkCache) {
        return getList(getDbSqlSession(), dbQueryName, parameter, cachedEntityMatcher, checkCache);
    }
    
    @SuppressWarnings("unchecked")
    protected List<EntityImpl> getList(DbSqlSession dbSqlSession, String dbQueryName, Object parameter,
            CachedEntityMatcher<EntityImpl> cachedEntityMatcher, boolean checkCache) {

        Collection<EntityImpl> result = dbSqlSession.selectList(dbQueryName, parameter);

        if (checkCache) {

            Collection<CachedEntity> cachedObjects = getEntityCache().findInCacheAsCachedObjects(getManagedEntityClass());

            if ((cachedObjects != null && cachedObjects.size() > 0) || getManagedEntitySubClasses() != null) {

                HashMap<String, EntityImpl> entityMap = new HashMap<>(result.size());

                // Database entities
                for (EntityImpl entity : result) {
                    entityMap.put(entity.getId(), entity);
                }

                // Cache entities
                if (cachedObjects != null && cachedEntityMatcher != null) {
                    for (CachedEntity cachedObject : cachedObjects) {
                        EntityImpl cachedEntity = (EntityImpl) cachedObject.getEntity();
                        if (cachedEntityMatcher.isRetained(result, cachedObjects, cachedEntity, parameter)) {
                            entityMap.put(cachedEntity.getId(), cachedEntity); // will overwrite db version with newer version
                        }
                    }
                }

                if (getManagedEntitySubClasses() != null && cachedEntityMatcher != null) {
                    for (Class<? extends EntityImpl> entitySubClass : getManagedEntitySubClasses()) {
                        Collection<CachedEntity> subclassCachedObjects = getEntityCache().findInCacheAsCachedObjects(entitySubClass);
                        if (subclassCachedObjects != null) {
                            for (CachedEntity subclassCachedObject : subclassCachedObjects) {
                                EntityImpl cachedSubclassEntity = (EntityImpl) subclassCachedObject.getEntity();
                                if (cachedEntityMatcher.isRetained(result, cachedObjects, cachedSubclassEntity, parameter)) {
                                    entityMap.put(cachedSubclassEntity.getId(), cachedSubclassEntity); // will overwrite db version with newer version
                                }
                            }
                        }
                    }
                }

                result = entityMap.values();

            }

        }

        // Remove entries which are already deleted
        if (result.size() > 0) {
            Iterator<EntityImpl> resultIterator = result.iterator();
            while (resultIterator.hasNext()) {
                if (dbSqlSession.isEntityToBeDeleted(resultIterator.next())) {
                    resultIterator.remove();
                }
            }
        }

        return new ArrayList<>(result);
    }

    @SuppressWarnings("unchecked")
    protected List<EntityImpl> getListFromCache(CachedEntityMatcher<EntityImpl> entityMatcher, Object parameter) {
        Collection<CachedEntity> cachedObjects = getEntityCache().findInCacheAsCachedObjects(getManagedEntityClass());

        DbSqlSession dbSqlSession = getDbSqlSession();

        List<EntityImpl> result = new ArrayList<>(cachedObjects != null ? cachedObjects.size() : 1);
        if (cachedObjects != null && entityMatcher != null) {
            for (CachedEntity cachedObject : cachedObjects) {
                EntityImpl cachedEntity = (EntityImpl) cachedObject.getEntity();
                if (entityMatcher.isRetained(null, cachedObjects, cachedEntity, parameter) && !dbSqlSession.isEntityToBeDeleted(cachedEntity)) {
                    result.add(cachedEntity);
                }
            }
        }

        if (getManagedEntitySubClasses() != null && entityMatcher != null) {
            for (Class<? extends EntityImpl> entitySubClass : getManagedEntitySubClasses()) {
                Collection<CachedEntity> subclassCachedObjects = getEntityCache().findInCacheAsCachedObjects(entitySubClass);
                if (subclassCachedObjects != null) {
                    for (CachedEntity subclassCachedObject : subclassCachedObjects) {
                        EntityImpl cachedSubclassEntity = (EntityImpl) subclassCachedObject.getEntity();
                        if (entityMatcher.isRetained(null, cachedObjects, cachedSubclassEntity, parameter) && !dbSqlSession.isEntityToBeDeleted(cachedSubclassEntity)) {
                            result.add(cachedSubclassEntity);
                        }
                    }
                }
            }
        }

        return result;
    }
    
    /**
     * Does a bulk delete, but also uses the provided {@link CachedEntityMatcher}
     * to look in the cache to mark the cached entities as deleted. 
     * (This is necessary if entities are inserted and deleted in the same operation). 
     */
    public void bulkDelete(String statement, CachedEntityMatcher<EntityImpl> cachedEntityMatcher, Object parameter) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        
        // Regular bulk delete
        dbSqlSession.delete(statement, parameter, getManagedEntityClass());
        
        // Special care needs to be taken for entities that have been in inserted in the same transaction
        // as when this bulk delete is issued: the entities needs to be added to the deleted list. 
        // This will not trigger an actual delete in the database, but will have as result that the entity will be
        // a) marked as deleted
        // b) the insert and the delete will cancel out each other, leaving only the bulk delete.
        deleteCachedEntities(dbSqlSession, cachedEntityMatcher, parameter);
    }

    protected void deleteCachedEntities(DbSqlSession dbSqlSession,  CachedEntityMatcher<EntityImpl> cachedEntityMatcher, Object parameter) {
        deleteCachedEntities(dbSqlSession, getEntityCache().findInCacheAsCachedObjects(getManagedEntityClass()), cachedEntityMatcher, parameter);
        if (getManagedEntitySubClasses() != null && cachedEntityMatcher != null) {
            for (Class<? extends EntityImpl> entitySubClass : getManagedEntitySubClasses()) {
                deleteCachedEntities(dbSqlSession, getEntityCache().findInCacheAsCachedObjects(entitySubClass), cachedEntityMatcher, parameter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void deleteCachedEntities(DbSqlSession dbSqlSession, Collection<CachedEntity> cachedObjects, 
            CachedEntityMatcher<EntityImpl> cachedEntityMatcher,  Object parameter) {
        if (cachedObjects != null && cachedEntityMatcher != null) {
            for (CachedEntity cachedObject : cachedObjects) {
                EntityImpl cachedEntity = (EntityImpl) cachedObject.getEntity();
                boolean entityMatches = cachedEntityMatcher.isRetained(null, cachedObjects, cachedEntity, parameter);
                if (cachedEntity.isInserted() && entityMatches) {
                    dbSqlSession.delete(cachedEntity);
                }
                if (entityMatches) {
                    cachedEntity.setDeleted(true);
                }
            }
        }
    }
    
    protected boolean isEntityInserted(DbSqlSession dbSqlSession, String entityLogicalName, String entityId) {
        Class<?> executionEntityClass = dbSqlSession.getDbSqlSessionFactory().getLogicalNameToClassMapping().get(entityLogicalName);
        return executionEntityClass != null && dbSqlSession.isEntityInserted(executionEntityClass, entityId);
    }

}
