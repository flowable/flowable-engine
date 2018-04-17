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

import org.apache.ibatis.session.SqlSession;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.AlwaysUpdatedPersistentObject;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DbSqlSession implements Session {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbSqlSession.class);

    public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };

    protected EntityCache entityCache;
    protected SqlSession sqlSession;
    protected DbSqlSessionFactory dbSqlSessionFactory;
    protected String connectionMetadataDefaultCatalog;
    protected String connectionMetadataDefaultSchema;

    protected Map<Class<? extends Entity>, Map<String, Entity>> insertedObjects = new HashMap<>();
    protected Map<Class<? extends Entity>, Map<String, Entity>> deletedObjects = new HashMap<>();
    protected Map<Class<? extends Entity>, List<BulkDeleteOperation>> bulkDeleteOperations = new HashMap<>();
    protected List<Entity> updatedObjects = new ArrayList<>();

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, EntityCache entityCache) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.entityCache = entityCache;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession();
    }

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, EntityCache entityCache, Connection connection, String catalog, String schema) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.entityCache = entityCache;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession(connection); // Note the use of connection param here, different from other constructor
        this.connectionMetadataDefaultCatalog = catalog;
        this.connectionMetadataDefaultSchema = schema;
        this.entityCache = entityCache;
    }

    // insert ///////////////////////////////////////////////////////////////////

    public void insert(Entity entity) {
        if (entity.getId() == null) {
            String id = Context.getCommandContext().getCurrentEngineConfiguration().getIdGenerator().getNextId();
            entity.setId(id);
        }

        Class<? extends Entity> clazz = entity.getClass();
        if (!insertedObjects.containsKey(clazz)) {
            insertedObjects.put(clazz, new LinkedHashMap<String, Entity>()); // order of insert is important, hence LinkedHashMap
        }

        insertedObjects.get(clazz).put(entity.getId(), entity);
        entityCache.put(entity, false); // False -> entity is inserted, so always changed
        entity.setInserted(true);
    }

    // update
    // ///////////////////////////////////////////////////////////////////

    public void update(Entity entity) {
        entityCache.put(entity, false); // false -> we don't store state, meaning it will always be seen as changed
        entity.setUpdated(true);
    }

    public int update(String statement, Object parameters) {
        String updateStatement = dbSqlSessionFactory.mapStatement(statement);
        return getSqlSession().update(updateStatement, parameters);
    }

    // delete
    // ///////////////////////////////////////////////////////////////////

    /**
     * Executes a {@link BulkDeleteOperation}, with the sql in the statement parameter.
     * The passed class determines when this operation will be executed: it will be executed depending on the place of the class in the {@link EntityDependencyOrder}.
     */
    public void delete(String statement, Object parameter, Class<? extends Entity> entityClass) {
        if (!bulkDeleteOperations.containsKey(entityClass)) {
            bulkDeleteOperations.put(entityClass, new ArrayList<BulkDeleteOperation>(1));
        }
        bulkDeleteOperations.get(entityClass).add(new BulkDeleteOperation(dbSqlSessionFactory.mapStatement(statement), parameter));
    }

    public void delete(Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (!deletedObjects.containsKey(clazz)) {
            deletedObjects.put(clazz, new LinkedHashMap<String, Entity>()); // order of insert is important, hence LinkedHashMap
        }
        deletedObjects.get(clazz).put(entity.getId(), entity);
        entity.setDeleted(true);
    }

    // select
    // ///////////////////////////////////////////////////////////////////

    @SuppressWarnings({ "rawtypes" })
    public List selectList(String statement) {
        return selectList(statement, null, -1, -1);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter) {
        return selectList(statement, parameter, -1, -1);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, Page page) {
        if (page != null) {
            return selectList(statement, parameter, page.getFirstResult(), page.getMaxResults());
        } else {
            return selectList(statement, parameter, -1, -1);
        }
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, ListQueryParameterObject parameter) {
        parameter.setDatabaseType(dbSqlSessionFactory.getDatabaseType());
        return selectListWithRawParameter(statement, parameter);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, int firstResult, int maxResults) {
        return selectList(statement, new ListQueryParameterObject(parameter, firstResult, maxResults));
    }

    @SuppressWarnings("rawtypes")
    public List selectListNoCacheCheck(String statement, Object parameter) {
        return selectListWithRawParameter(statement, new ListQueryParameterObject(parameter, -1, -1), false);
    }

    @SuppressWarnings({ "rawtypes" })
    public List selectListWithRawParameterNoCacheCheck(String statement, Object parameter) {
        return selectListWithRawParameter(statement, parameter, false);
    }

    @SuppressWarnings({ "rawtypes" })
    public List selectListWithRawParameterNoCacheCheck(String statement, ListQueryParameterObject parameter) {
        parameter.setDatabaseType(dbSqlSessionFactory.getDatabaseType());
        return selectListWithRawParameter(statement, parameter, false);
    }

    @SuppressWarnings("rawtypes")
    public List selectListNoCacheCheck(String statement, ListQueryParameterObject parameter) {
        ListQueryParameterObject parameterToUse = parameter;
        if (parameterToUse == null) {
            parameterToUse = new ListQueryParameterObject();
        }
        return selectListWithRawParameter(statement, parameter, false);
    }

    @SuppressWarnings("rawtypes")
    public List selectListWithRawParameter(String statement, Object parameter) {
        // All other selectList methods eventually end up here, passing it into the method
        // with the useCache parameter. By default true, which means everything is cached.
        // Dedicated xNoCacheCheck methods will pass a false for that setting.
        return selectListWithRawParameter(statement, parameter, true);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List selectListWithRawParameter(String statement, Object parameter, boolean useCache) {
        statement = dbSqlSessionFactory.mapStatement(statement);
        List loadedObjects = sqlSession.selectList(statement, parameter);
        if (useCache) {
            return cacheLoadOrStore(loadedObjects);
        } else {
            return loadedObjects;
        }
    }

    public Object selectOne(String statement, Object parameter) {
        statement = dbSqlSessionFactory.mapStatement(statement);
        Object result = sqlSession.selectOne(statement, parameter);
        if (result instanceof Entity) {
            Entity loadedObject = (Entity) result;
            result = cacheLoadOrStore(loadedObject);
        }
        return result;
    }

    public <T extends Entity> T selectById(Class<T> entityClass, String id) {
        return selectById(entityClass, id, true);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T selectById(Class<T> entityClass, String id, boolean useCache) {
        T entity = null;

        if (useCache) {
            entity = entityCache.findInCache(entityClass, id);
            if (entity != null) {
                return entity;
            }
        }

        String selectStatement = dbSqlSessionFactory.getSelectStatement(entityClass);
        selectStatement = dbSqlSessionFactory.mapStatement(selectStatement);
        entity = (T) sqlSession.selectOne(selectStatement, id);
        if (entity == null) {
            return null;
        }

        entityCache.put(entity, true); // true -> store state so we can see later if it is updated later on
        return entity;
    }

    // internal session cache
    // ///////////////////////////////////////////////////

    @SuppressWarnings("rawtypes")
    protected List cacheLoadOrStore(List<Object> loadedObjects) {
        if (loadedObjects.isEmpty()) {
            return loadedObjects;
        }
        if (!(loadedObjects.get(0) instanceof Entity)) {
            return loadedObjects;
        }

        List<Entity> filteredObjects = new ArrayList<>(loadedObjects.size());
        for (Object loadedObject : loadedObjects) {
            Entity cachedEntity = cacheLoadOrStore((Entity) loadedObject);
            filteredObjects.add(cachedEntity);
        }
        return filteredObjects;
    }

    /**
     * Returns the object in the cache. If this object was loaded before, then the original object is returned (the cached version is more recent). If this is the first time this object is loaded,
     * then the loadedObject is added to the cache.
     */
    protected Entity cacheLoadOrStore(Entity entity) {
        Entity cachedEntity = entityCache.findInCache(entity.getClass(), entity.getId());
        if (cachedEntity != null) {
            return cachedEntity;
        }
        entityCache.put(entity, true);
        return entity;
    }

    // flush
    // ////////////////////////////////////////////////////////////////////

    @Override
    public void flush() {
        determineUpdatedObjects(); // Needs to be done before the removeUnnecessaryOperations, as removeUnnecessaryOperations will remove stuff from the cache
        removeUnnecessaryOperations();

        if (LOGGER.isDebugEnabled()) {
            debugFlush();
        }

        flushInserts();
        flushUpdates();
        flushDeletes();
    }

    /**
     * Clears all deleted and inserted objects from the cache, and removes inserts and deletes that cancel each other.
     *
     * Also removes deletes with duplicate ids.
     */
    protected void removeUnnecessaryOperations() {

        for (Class<? extends Entity> entityClass : deletedObjects.keySet()) {

            // Collect ids of deleted entities + remove duplicates
            Set<String> ids = new HashSet<>();
            Iterator<Entity> entitiesToDeleteIterator = deletedObjects.get(entityClass).values().iterator();
            while (entitiesToDeleteIterator.hasNext()) {
                Entity entityToDelete = entitiesToDeleteIterator.next();
                if (!ids.contains(entityToDelete.getId())) {
                    ids.add(entityToDelete.getId());
                } else {
                    entitiesToDeleteIterator.remove(); // Removing duplicate deletes
                }
            }

            // Now we have the deleted ids, we can remove the inserted objects (as they cancel each other)
            for (String id : ids) {
                if (insertedObjects.containsKey(entityClass) && insertedObjects.get(entityClass).containsKey(id)) {
                    insertedObjects.get(entityClass).remove(id);
                    deletedObjects.get(entityClass).remove(id);
                }
            }

        }
    }

    public void determineUpdatedObjects() {
        updatedObjects = new ArrayList<>();
        Map<Class<?>, Map<String, CachedEntity>> cachedObjects = entityCache.getAllCachedEntities();
        for (Class<?> clazz : cachedObjects.keySet()) {

            Map<String, CachedEntity> classCache = cachedObjects.get(clazz);
            for (CachedEntity cachedObject : classCache.values()) {

                Entity cachedEntity = cachedObject.getEntity();

                // Executions are stored as a hierarchical tree, and updates are important to execute
                // even when the execution are deleted, as they can change the parent-child relationships.
                // For the other entities, this is not applicable and an update can be discarded when an update follows.

                if (!isEntityInserted(cachedEntity) &&
                        (cachedEntity instanceof AlwaysUpdatedPersistentObject || !isEntityToBeDeleted(cachedEntity)) &&
                        cachedObject.hasChanged()) {

                    updatedObjects.add(cachedEntity);
                }
            }
        }
    }

    protected void debugFlush() {
        LOGGER.debug("Flushing dbSqlSession");
        int nrOfInserts = 0;
        int nrOfUpdates = 0;
        int nrOfDeletes = 0;
        for (Map<String, Entity> insertedObjectMap : insertedObjects.values()) {
            for (Entity insertedObject : insertedObjectMap.values()) {
                LOGGER.debug("  insert {}", insertedObject);
                nrOfInserts++;
            }
        }
        for (Entity updatedObject : updatedObjects) {
            LOGGER.debug("  update {}", updatedObject);
            nrOfUpdates++;
        }
        for (Map<String, Entity> deletedObjectMap : deletedObjects.values()) {
            for (Entity deletedObject : deletedObjectMap.values()) {
                LOGGER.debug("  delete {} with id {}", deletedObject, deletedObject.getId());
                nrOfDeletes++;
            }
        }
        for (Collection<BulkDeleteOperation> bulkDeleteOperationList : bulkDeleteOperations.values()) {
            for (BulkDeleteOperation bulkDeleteOperation : bulkDeleteOperationList) {
                LOGGER.debug("  {}", bulkDeleteOperation);
                nrOfDeletes++;
            }
        }
        LOGGER.debug("flush summary: {} insert, {} update, {} delete.", nrOfInserts, nrOfUpdates, nrOfDeletes);
        LOGGER.debug("now executing flush...");
    }

    public boolean isEntityInserted(Entity entity) {
        return isEntityInserted(entity.getClass(), entity.getId());
    }
    
    public boolean isEntityInserted(Class<?> entityClass, String entityId) {
        return insertedObjects.containsKey(entityClass)
                && insertedObjects.get(entityClass).containsKey(entityId);
    }

    public boolean isEntityToBeDeleted(Entity entity) {
        return (deletedObjects.containsKey(entity.getClass())
                && deletedObjects.get(entity.getClass()).containsKey(entity.getId())) || entity.isDeleted();
    }

    protected void flushInserts() {

        if (insertedObjects.size() == 0) {
            return;
        }

        // Handle in entity dependency order
        for (Class<? extends Entity> entityClass : dbSqlSessionFactory.getInsertionOrder()) {
            if (insertedObjects.containsKey(entityClass)) {
                flushInsertEntities(entityClass, insertedObjects.get(entityClass).values());
                insertedObjects.remove(entityClass);
            }
        }

        // Next, in case of custom entities or we've screwed up and forgotten some entity
        if (insertedObjects.size() > 0) {
            for (Class<? extends Entity> entityClass : insertedObjects.keySet()) {
                flushInsertEntities(entityClass, insertedObjects.get(entityClass).values());
            }
        }

        insertedObjects.clear();
    }

    protected void flushInsertEntities(Class<? extends Entity> entityClass, Collection<Entity> entitiesToInsert) {
        if (entitiesToInsert.size() == 1) {
            flushRegularInsert(entitiesToInsert.iterator().next(), entityClass);
        } else if (Boolean.FALSE.equals(dbSqlSessionFactory.isBulkInsertable(entityClass))) {
            for (Entity entity : entitiesToInsert) {
                flushRegularInsert(entity, entityClass);
            }
        } else {
            flushBulkInsert(entitiesToInsert, entityClass);
        }
    }

    protected void flushRegularInsert(Entity entity, Class<? extends Entity> clazz) {
        String insertStatement = dbSqlSessionFactory.getInsertStatement(entity);
        insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);

        if (insertStatement == null) {
            throw new FlowableException("no insert statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        LOGGER.debug("inserting: {}", entity);
        sqlSession.insert(insertStatement, entity);

        // See https://activiti.atlassian.net/browse/ACT-1290
        if (entity instanceof HasRevision) {
            incrementRevision(entity);
        }
    }

    protected void flushBulkInsert(Collection<Entity> entities, Class<? extends Entity> clazz) {
        String insertStatement = dbSqlSessionFactory.getBulkInsertStatement(clazz);
        insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);

        if (insertStatement == null) {
            throw new FlowableException("no insert statement for " + entities.iterator().next().getClass() + " in the ibatis mapping files");
        }

        Iterator<Entity> entityIterator = entities.iterator();
        Boolean hasRevision = null;

        while (entityIterator.hasNext()) {
            List<Entity> subList = new ArrayList<>();
            int index = 0;
            while (entityIterator.hasNext() && index < dbSqlSessionFactory.getMaxNrOfStatementsInBulkInsert()) {
                Entity entity = entityIterator.next();
                subList.add(entity);

                if (hasRevision == null) {
                    hasRevision = entity instanceof HasRevision;
                }
                index++;
            }
            sqlSession.insert(insertStatement, subList);
        }

        if (hasRevision != null && hasRevision) {
            entityIterator = entities.iterator();
            while (entityIterator.hasNext()) {
                incrementRevision(entityIterator.next());
            }
        }

    }

    protected void incrementRevision(Entity insertedObject) {
        HasRevision revisionEntity = (HasRevision) insertedObject;
        if (revisionEntity.getRevision() == 0) {
            revisionEntity.setRevision(revisionEntity.getRevisionNext());
        }
    }

    protected void flushUpdates() {
        for (Entity updatedObject : updatedObjects) {
            String updateStatement = dbSqlSessionFactory.getUpdateStatement(updatedObject);
            updateStatement = dbSqlSessionFactory.mapStatement(updateStatement);

            if (updateStatement == null) {
                throw new FlowableException("no update statement for " + updatedObject.getClass() + " in the ibatis mapping files");
            }

            LOGGER.debug("updating: {}", updatedObject);

            int updatedRecords = sqlSession.update(updateStatement, updatedObject);
            if (updatedRecords == 0) {
                throw new FlowableOptimisticLockingException(updatedObject + " was updated by another transaction concurrently");
            }

            // See https://activiti.atlassian.net/browse/ACT-1290
            if (updatedObject instanceof HasRevision) {
                ((HasRevision) updatedObject).setRevision(((HasRevision) updatedObject).getRevisionNext());
            }

        }
        updatedObjects.clear();
    }

    protected void flushDeletes() {

        if (deletedObjects.size() == 0 && bulkDeleteOperations.size() == 0) {
            return;
        }

        // Handle in entity dependency order
        for (Class<? extends Entity> entityClass : dbSqlSessionFactory.getDeletionOrder()) {
            if (deletedObjects.containsKey(entityClass)) {
                flushDeleteEntities(entityClass, deletedObjects.get(entityClass).values());
                deletedObjects.remove(entityClass);
            }
            flushBulkDeletes(entityClass);
        }

        // Next, in case of custom entities or we've screwed up and forgotten some entity
        if (deletedObjects.size() > 0) {
            for (Class<? extends Entity> entityClass : deletedObjects.keySet()) {
                flushDeleteEntities(entityClass, deletedObjects.get(entityClass).values());
                flushBulkDeletes(entityClass);
            }
        }

        deletedObjects.clear();
    }

    protected void flushBulkDeletes(Class<? extends Entity> entityClass) {
        // Bulk deletes
        if (bulkDeleteOperations.containsKey(entityClass)) {
            for (BulkDeleteOperation bulkDeleteOperation : bulkDeleteOperations.get(entityClass)) {
                bulkDeleteOperation.execute(sqlSession, entityClass);
            }
        }
    }

    protected void flushDeleteEntities(Class<? extends Entity> entityClass, Collection<Entity> entitiesToDelete) {
        for (Entity entity : entitiesToDelete) {
            String deleteStatement = dbSqlSessionFactory.getDeleteStatement(entity.getClass());
            deleteStatement = dbSqlSessionFactory.mapStatement(deleteStatement);
            if (deleteStatement == null) {
                throw new FlowableException("no delete statement for " + entity.getClass() + " in the ibatis mapping files");
            }

            // It only makes sense to check for optimistic locking exceptions
            // for objects that actually have a revision
            if (entity instanceof HasRevision) {
                int nrOfRowsDeleted = sqlSession.delete(deleteStatement, entity);
                if (nrOfRowsDeleted == 0) {
                    throw new FlowableOptimisticLockingException(entity + " was updated by another transaction concurrently");
                }
            } else {
                sqlSession.delete(deleteStatement, entity);
            }
        }
    }

    @Override
    public void close() {
        sqlSession.close();
    }

    public void commit() {
        sqlSession.commit();
    }

    public void rollback() {
        sqlSession.rollback();
    }

    public <T> T getCustomMapper(Class<T> type) {
        return sqlSession.getMapper(type);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public SqlSession getSqlSession() {
        return sqlSession;
    }

    public DbSqlSessionFactory getDbSqlSessionFactory() {
        return dbSqlSessionFactory;
    }

    public String getConnectionMetadataDefaultCatalog() {
        return connectionMetadataDefaultCatalog;
    }

    public void setConnectionMetadataDefaultCatalog(String connectionMetadataDefaultCatalog) {
        this.connectionMetadataDefaultCatalog = connectionMetadataDefaultCatalog;
    }

    public String getConnectionMetadataDefaultSchema() {
        return connectionMetadataDefaultSchema;
    }

    public void setConnectionMetadataDefaultSchema(String connectionMetadataDefaultSchema) {
        this.connectionMetadataDefaultSchema = connectionMetadataDefaultSchema;
    }

}
