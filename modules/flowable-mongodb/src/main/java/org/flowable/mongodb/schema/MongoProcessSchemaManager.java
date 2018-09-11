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
package org.flowable.mongodb.schema;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bson.Document;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.mongodb.persistence.MongoDbSession;
import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

/**
 * @author Joram Barrez
 */
public class MongoProcessSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoProcessSchemaManager.class);
    
    @Override
    public void schemaCreate() {
        MongoDbProcessEngineConfiguration engineConfiguration = getEngineConfiguration();
        
        // Collections can't be created in a transaction (see https://docs.mongodb.com/manual/core/transactions/)
        Set<String> collections = new HashSet<>();
        MongoIterable<String> existingCollections = engineConfiguration.getMongoDatabase().listCollectionNames();
        if (existingCollections != null) {
            for (String collectionName : existingCollections) {
                collections.add(collectionName);
            }
        }
        
        for (String name : getAllCollectionNames()) {
            if (!collections.contains(name)) {
                engineConfiguration.getMongoDatabase().createCollection(name);
            }
        };
    }

    @Override
    public void schemaDrop() {
        LOGGER.info("Dropping all MongoDB collections in the database");
        MongoDatabase mongoDatabase = getEngineConfiguration().getMongoDatabase();
        for (String collectionName : getAllCollectionNames()) {
            MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
            if (collection != null) {
                collection.drop();
            }
        }
    }

    @Override
    public String schemaUpdate() {
        schemaCreate();
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        
    }
    
    protected Collection<String> getAllCollectionNames() {
        MongoDbSessionFactory mongoDbSessionFactory = (MongoDbSessionFactory) getEngineConfiguration().getSessionFactories().get(MongoDbSession.class);
        return mongoDbSessionFactory.getCollectionNames();
    }
    
    protected MongoDbProcessEngineConfiguration getEngineConfiguration() {
        MongoDbProcessEngineConfiguration engineConfiguration = (MongoDbProcessEngineConfiguration) CommandContextUtil.getProcessEngineConfiguration();
        return engineConfiguration;
    }

}
