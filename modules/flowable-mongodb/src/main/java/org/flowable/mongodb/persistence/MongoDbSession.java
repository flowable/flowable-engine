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
package org.flowable.mongodb.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.AlwaysUpdatedPersistentObject;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

/**
 * @author Joram Barrez
 */
public class MongoDbSession implements Session {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbSession.class);

    protected MongoDbSessionFactory mongoDbSessionFactory;
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    
    protected ClientSession clientSession;
    
    protected EntityCache entityCache;
    protected Map<Class<? extends Entity>, Map<String, Entity>> insertedObjects = new HashMap<>();
    protected Map<Class<? extends Entity>, Map<String, Entity>> deletedObjects = new HashMap<>();
    protected List<Entity> updatedObjects = new ArrayList<>();

    public MongoDbSession(MongoDbSessionFactory mongoDbSessionFactory, MongoClient mongoClient, MongoDatabase mongoDatabase, EntityCache entityCache) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoDatabase;
        this.entityCache = entityCache;
        
        // TODO: transaction shouldn't be started when externally managed
        startTransaction();
    }
    
    public void startTransaction() {
        clientSession = mongoClient.startSession();
        clientSession.startTransaction();
    }

    @Override
    public void close() {
        if (clientSession != null) {
            clientSession.close();
        }
    }
    
    public void insertOne(Entity entity) {
        if (entity.getId() == null) {
            String id = Context.getCommandContext().getCurrentEngineConfiguration().getIdGenerator().getNextId();
            entity.setId(id);
        }
        
        Class<? extends Entity> clazz = entity.getClass();
        if (!insertedObjects.containsKey(clazz)) {
            insertedObjects.put(clazz, new LinkedHashMap<>()); // order of insert is important, hence LinkedHashMap
        }

        insertedObjects.get(clazz).put(entity.getId(), entity);
        entityCache.put(entity, false); // False -> entity is inserted, so always changed
        entity.setInserted(true);
    }
    
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

    @SuppressWarnings("unchecked")
    protected void flushInserts() {
        if (insertedObjects.size() == 0) {
            return;
        }
        
        for (Class<? extends Entity> clazz : insertedObjects.keySet()) {
            
            LOGGER.debug("inserting type: {}", clazz);
            
            MongoCollection<Document> mongoDbCollection = getMongoDatabase().getCollection(mongoDbSessionFactory.getClassToCollectionsMap().get(clazz));
            
            Map<String, ? extends Entity> entities = insertedObjects.get(clazz);
            EntityToDocumentMapper entityMapper = mongoDbSessionFactory.getMapperForEntityClass(clazz);
            for (Entity entity : entities.values()) {
                Document document = entityMapper.toDocument(entity);
                mongoDbCollection.insertOne(clientSession, document);
            }
        }
    }
    
    protected void flushUpdates() {

        // Regular updates
        for (Entity updatedObject : updatedObjects) {
            
            LOGGER.debug("updating: {}", updatedObject);

            Class<?> entityClass = updatedObject.getClass();
            String collectionName = mongoDbSessionFactory.getClassToCollectionsMap().get(entityClass);
            BasicDBObject updateObject = mongoDbSessionFactory.getDataManagerForCollection(collectionName).createUpdateObject(updatedObject);
            if (updateObject != null) {
                MongoCollection<Document> collection = getMongoDatabase().getCollection(collectionName);
                UpdateResult updateResult = collection
                    .updateOne(clientSession, Filters.eq("_id", updatedObject.getId()), new Document().append("$set", updateObject));
                if (updateResult.getModifiedCount() == 0) {
                    throw new FlowableOptimisticLockingException(updatedObject + " was updated by another transaction concurrently");
                }
            }
            
            if (updatedObject instanceof HasRevision) {
                ((HasRevision) updatedObject).setRevision(((HasRevision) updatedObject).getRevisionNext());
            }

        }
        updatedObjects.clear();

//        // MongoDb specific updates
//        if (!mongoDbSpecificUpdates.isEmpty()) {
//            for (String collection : mongoDbSpecificUpdates.keySet()) {
//                List<SingleEntityUpdate> basicDBObjects = mongoDbSpecificUpdates.get(collection);
//                basicDBObjects.forEach(singleEntityUpdate -> getMongoDatabase().getCollection(collection)
//                    .updateMany(clientSession, Filters.eq("_id", singleEntityUpdate.getId()), new Document().append("$set", singleEntityUpdate.getBasicDBObject())));
//            }
//        }
    }
    
    protected void flushDeletes() {
        if (deletedObjects.size() == 0) {
            return;
        }
        
        for (Class<? extends Entity> clazz : deletedObjects.keySet()) {
            
            MongoCollection<Document> mongoDbCollection = getMongoDatabase().getCollection(mongoDbSessionFactory.getClassToCollectionsMap().get(clazz));
            Map<String, ? extends Entity> entities = deletedObjects.get(clazz);
            for (Entity entity : entities.values()) {
                mongoDbCollection.deleteOne(clientSession, Filters.eq("_id", entity.getId()));
            }
        }
    }
    
    public <T> List<T> find(String collection, Bson bsonFilter) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter);
        return mapToEntities(collection, documents);
    }
    
    public <T> List<T> find(String collection, Bson bsonFilter, Bson bsonSort) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter, bsonSort);
        return mapToEntities(collection, documents);
    }
    
    public <T> List<T> find(String collection, Bson bsonFilter, Bson bsonSort, int limit) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter, bsonSort, limit);
        return mapToEntities(collection, documents);
    }
    
    public <T> List<T> find(String collection, Bson bsonFilter, Object parameter, Class<? extends Entity> entityClass, CachedEntityMatcher<Entity> cachedEntityMatcher) {
        return find(collection, bsonFilter, parameter, entityClass, cachedEntityMatcher, true);
    }
    
    @SuppressWarnings("unchecked")
    public <T> List<T> find(String collection, Bson bsonFilter, Object parameter, Class<? extends Entity> entityClass, CachedEntityMatcher<Entity> cachedEntityMatcher, boolean checkCache) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter);
        Collection<Entity> dbEntities = mapToEntitiesType(collection, documents);

        if (checkCache) {

            Collection<CachedEntity> cachedObjects = getEntityCache().findInCacheAsCachedObjects(entityClass);

            if ((cachedObjects != null && cachedObjects.size() > 0)) {

                HashMap<String, Entity> entityMap = new HashMap<>(dbEntities.size());

                // Database entities
                for (Entity entity : dbEntities) {
                    entityMap.put(entity.getId(), entity);
                }

                // Cache entities
                if (cachedObjects != null && cachedEntityMatcher != null) {
                    for (CachedEntity cachedObject : cachedObjects) {
                        Entity cachedEntity = cachedObject.getEntity();
                        if (cachedEntityMatcher.isRetained(dbEntities, cachedObjects, cachedEntity, parameter)) {
                            entityMap.put(cachedEntity.getId(), cachedEntity); // will overwrite db version with newer version
                        }
                    }
                }

                dbEntities = entityMap.values();
            }
        }

        // Remove entries which are already deleted
        if (dbEntities.size() > 0) {
            dbEntities.removeIf(this::isEntityToBeDeleted);
        }

        return (List<T>) new ArrayList<>(dbEntities);
    }
    
    public <T> List<T> findFromCache(CachedEntityMatcher<Entity> entityMatcher, Object parameter, Class<? extends Entity> entityClass) {
        Collection<CachedEntity> cachedObjects = getEntityCache().findInCacheAsCachedObjects(entityClass);

        List<Entity> result = new ArrayList<>(cachedObjects != null ? cachedObjects.size() : 1);
        if (cachedObjects != null && entityMatcher != null) {
            for (CachedEntity cachedObject : cachedObjects) {
                Entity cachedEntity = cachedObject.getEntity();
                if (entityMatcher.isRetained(null, cachedObjects, cachedEntity, parameter) && !isEntityToBeDeleted(cachedEntity)) {
                    result.add(cachedEntity);
                }
            }
        }

        return (List<T>) result;
    }

    public <T> T findOne(String collection, Bson bsonFilter) {
        return findOne(collection, bsonFilter, null, -1);
    }

    public <T> T findOne(String collection, Bson bsonFilter, Bson sort, int limit) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter, sort, limit);
        if (documents != null) {
            T entity = mapToEntity(collection, documents);
            if (entity instanceof Entity) {
                String id = ((Entity) entity).getId();
                T cachedEntity = (T) entityCache.findInCache(mongoDbSessionFactory.getClassForCollection(collection), id);
                if (cachedEntity != null) {
                    return cachedEntity;
                }

                entityCache.put((Entity) entity, true); // true -> store state so we can see later if it is updated later on
            }

            return entity;
        }
        return null;
    }
    
    public <T> T mapToEntity(String collection, FindIterable<Document> documents) {
        Iterator<Document> iterator = documents.iterator();
        if (iterator.hasNext()) {
            Document document = iterator.next();
            if (document != null) {
                EntityToDocumentMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
                return (T) entityMapper.fromDocument(document);
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> List<T> mapToEntities(String collection, FindIterable<Document> documents) {
        EntityToDocumentMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
        List<Object> entities = new ArrayList<>();
        for (Document document : documents) {
            entities.add((T) entityMapper.fromDocument(document));
        }
        
        return cacheLoadOrStore(entities);
    }
    
    public List<Entity> mapToEntitiesType(String collection, FindIterable<Document> documents) {
        EntityToDocumentMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
        List<Object> entities = new ArrayList<>();
        for (Document document : documents) {
            entities.add(entityMapper.fromDocument(document));
        }
        
        return cacheLoadOrStore(entities);
    }
    
    public FindIterable<Document> findDocuments(String collection, Bson bsonFilter) {
        return findDocuments(collection, bsonFilter, null);
    }
    
    public FindIterable<Document> findDocuments(String collection, Bson bsonFilter, Bson bsonSort) {
        return findDocuments(collection, bsonFilter, bsonSort, 0);
    }
    
    public FindIterable<Document> findDocuments(String collection, Bson bsonFilter, Bson bsonSort, int limit) {
        MongoCollection<Document> mongoDbCollection = getCollection(collection);
        FindIterable<Document> documentResult = null;
        if (bsonFilter != null) {
            documentResult = mongoDbCollection.find(clientSession, bsonFilter);
        } else {
            documentResult = mongoDbCollection.find(clientSession);
        }
        
        if (bsonSort != null) {
            documentResult = documentResult.sort(bsonSort);
        }
        
        if (limit > 0) {
            documentResult = documentResult.limit(limit);
        }
        
        return documentResult;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T findOne(String collection, String id) {
        
        T entity = (T) entityCache.findInCache(mongoDbSessionFactory.getClassForCollection(collection), id);
        if (entity != null) {
            return entity;
        }
        
        Document document = findOneDocument(collection, id);
        if (document == null) {
            return null;
        }
        
        EntityToDocumentMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
        entity = (T) entityMapper.fromDocument(document);
        
        entityCache.put((Entity) entity, true); // true -> store state so we can see later if it is updated later on
        return entity;
    }
    
    public Document findOneDocument(String collection, String id) {
        Bson filter = Filters.eq("_id", id);
        FindIterable<Document> documents = findDocuments(collection, filter);
        if (documents != null) {
            // TODO: caching
            return documents.first();
        }
        return null;
    }
    
    public long count(String collection, Bson bsonFilter) {
        MongoCollection<Document> mongoDbCollection = getCollection(collection);
        if (bsonFilter != null) {
            return mongoDbCollection.countDocuments(clientSession, bsonFilter);
        } else {
            return mongoDbCollection.countDocuments(clientSession);
        }
    }
    
    public void update(Entity entity) {
        entityCache.put(entity, false); // false -> we don't store state, meaning it will always be seen as changed
        entity.setUpdated(true);
    }
    
    public void delete(String collection, Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (!deletedObjects.containsKey(clazz)) {
            deletedObjects.put(clazz, new LinkedHashMap<>()); // order of insert is important, hence LinkedHashMap
        }
        deletedObjects.get(clazz).put(entity.getId(), entity);
        entity.setDeleted(true);
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

                if (!isEntityInserted(cachedEntity) && !isEntityToBeDeleted(cachedEntity) && 
                        (cachedEntity instanceof AlwaysUpdatedPersistentObject || cachedObject.hasChanged())) {

                    updatedObjects.add(cachedEntity);
                }
            }
        }
    }
    
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
    
    /**
     * TODO: copied from DbSqlSession, could be extracted in a common place.
     */
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
     * TODO: copied from DbSqlSession, could be extracted in a common place.,
     * 
     * Returns the object in the cache. If this object was loaded before, then the original object is returned (the cached version is more recent). 
     * If this is the first time this object is loaded, then the loadedObject is added to the cache.
     */
    protected Entity cacheLoadOrStore(Entity entity) {
        Entity cachedEntity = entityCache.findInCache(entity.getClass(), entity.getId());
        if (cachedEntity != null) {
            return cachedEntity;
        }
        entityCache.put(entity, true);
        return entity;
    }
    
    public MongoCollection<Document> getCollection(String collection) {
        return getMongoDatabase().getCollection(collection);
    }

    public MongoDbSessionFactory getMongoDbSessionFactory() {
        return mongoDbSessionFactory;
    }

    public void setMongoDbSessionFactory(MongoDbSessionFactory mongoDbSessionFactory) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public void setMongoDatabase(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public EntityCache getEntityCache() {
        return entityCache;
    }

    public void setEntityCache(EntityCache entityCache) {
        this.entityCache = entityCache;
    }

}
