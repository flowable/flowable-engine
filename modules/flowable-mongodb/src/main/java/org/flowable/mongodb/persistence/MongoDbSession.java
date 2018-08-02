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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;

import com.mongodb.MongoClient;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbSession implements Session {
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    
    protected ClientSession clientSession;
    
    protected EntityCache entityCache;
    protected Map<Class<? extends Entity>, Map<String, Entity>> insertedObjects = new HashMap<>();
    protected Map<Class<? extends Entity>, Map<String, Entity>> deletedObjects = new HashMap<>();
    
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
        flushInserts();
        flushDeletes();
    }

    @SuppressWarnings("unchecked")
    protected void flushInserts() {
        if (insertedObjects.size() == 0) {
            return;
        }
        
        for (Class<? extends Entity> clazz : insertedObjects.keySet()) {
            
            MongoCollection<Document> mongoDbCollection = getMongoDatabase().getCollection(mongoDbSessionFactory.getCollections().get(clazz));
            
            Map<String, ? extends Entity> entities = insertedObjects.get(clazz);
            EntityMapper entityMapper = mongoDbSessionFactory.getMapperForEntityClass(clazz);
            for (Entity entity : entities.values()) {
                Document document = entityMapper.toDocument(entity);
                mongoDbCollection.insertOne(clientSession, document);
            }
        }
    }
    
    protected void flushDeletes() {
        if (deletedObjects.size() == 0) {
            return;
        }
        
        for (Class<? extends Entity> clazz : deletedObjects.keySet()) {
            
            MongoCollection<Document> mongoDbCollection = getMongoDatabase().getCollection(mongoDbSessionFactory.getCollections().get(clazz));
            Map<String, ? extends Entity> entities = deletedObjects.get(clazz);
            EntityMapper entityMapper = mongoDbSessionFactory.getMapperForEntityClass(clazz);
            for (Entity entity : entities.values()) {
                mongoDbCollection.deleteOne(Filters.eq("_id", entity.getId()));
            }
        }
    }
    
    public <T> List<T> find(String collection, Bson bsonFilter) {
        FindIterable<Document> documents = findDocuments(collection, bsonFilter);
        return mapToEntities(collection, documents);
    }
    
    public <T> T mapToEntity(String collection, FindIterable<Document> documents) {
        Iterator<Document> iterator = documents.iterator();
        if (iterator.hasNext()) {
            Document document = iterator.next();
            if (document != null) {
                EntityMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
                return (T) entityMapper.fromDocument(document);
            }
        }
        return null;
    }
    
    public <T> List<T> mapToEntities(String collection, FindIterable<Document> documents) {
        EntityMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
        List<T> entities = new ArrayList<>();
        for (Document document : documents) {
            entities.add((T) entityMapper.fromDocument(document));
        }
        return entities;
    }
    
    public FindIterable<Document> findDocuments(String collection, Bson bsonFilter) {
        MongoCollection<Document> mongoDbCollection = getCollection(collection);
        if (bsonFilter != null) {
            return mongoDbCollection.find(clientSession, bsonFilter);
        } else {
            return mongoDbCollection.find(clientSession);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T findOne(String collection, String id) {
        
        T entity = (T) entityCache.findInCache(mongoDbSessionFactory.getClassForCollection(collection), id);
        if (entity != null) {
            return entity;
        }
        
        Document document = findOneDocument(collection, id);
        EntityMapper<? extends Entity> entityMapper = mongoDbSessionFactory.getCollectionToMapper().get(collection);
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
    
    public void delete(String collection, Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (!deletedObjects.containsKey(clazz)) {
            deletedObjects.put(clazz, new LinkedHashMap<>()); // order of insert is important, hence LinkedHashMap
        }
        deletedObjects.get(clazz).put(entity.getId(), entity);
        entity.setDeleted(true);
    }
    
    protected MongoCollection<Document> getCollection(String collection) {
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
    
    
    
}
