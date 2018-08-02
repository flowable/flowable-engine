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

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Session;
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
    
    public MongoDbSession(MongoDbSessionFactory mongoDbSessionFactory, MongoClient mongoClient, MongoDatabase mongoDatabase) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoDatabase;
        
        // TODO: transaction shouldn't be started when externally managed
        startTransaction();
    }
    
    public void startTransaction() {
        clientSession = mongoClient.startSession();
        clientSession.startTransaction();
    }

    @Override
    public void flush() {
        
    }

    @Override
    public void close() {
        if (clientSession != null) {
            clientSession.close();
        }
    }
    
    public void insertOne(Entity entity, String collection, Document document) {
        if (!document.containsKey("_id")) {
            String id = Context.getCommandContext().getCurrentEngineConfiguration().getIdGenerator().getNextId();
            document.append("_id", id);
            
            entity.setId(id);
        }
        
        MongoCollection<Document> mongoDbCollection = getMongoDatabase().getCollection(collection);
        mongoDbCollection.insertOne(clientSession, document);
    }
    
    public FindIterable<Document> find(String collection, Bson bsonFilter) {
        MongoCollection<Document> mongoDbCollection = getCollection(collection);
        if (bsonFilter != null) {
            return mongoDbCollection.find(clientSession, bsonFilter);
        } else {
            return mongoDbCollection.find(clientSession);
        }
    }
    
    public Document findOne(String collection, String id) {
        Bson filter = Filters.eq("_id", id);
        FindIterable<Document> documents = find(collection, filter);
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
    
    public void delete(String collection, String id) {
        MongoCollection<Document> mongoDbCollection = getCollection(collection);
        mongoDbCollection.deleteOne(Filters.eq("_id", id));
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
