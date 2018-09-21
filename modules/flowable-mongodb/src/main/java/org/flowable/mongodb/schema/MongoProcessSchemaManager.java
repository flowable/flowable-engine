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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.mongodb.persistence.MongoDbSession;
import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.flowable.mongodb.persistence.manager.MongoDbEventSubscriptionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbExecutionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricActivityInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricIdentityLinkDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricProcessInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricTaskInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricVariableInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbIdentityLinkDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbResourceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTaskDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTimerJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbVariableInstanceDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;

/**
 * Note that the term 'schema' is not the same as for a relational database.
 * MongoDB doesn't have a schema that forces the data in a strict structure.
 * 
 * What this class does is:
 * - Making sure all collections needed by the Flowable Process engine are created.
 * - Making sure the necessary indices have been created on those collections.
 * 
 * Note we're not using the schema validation feature of MongoDB (see https://docs.mongodb.com/manual/core/schema-validation/).
 * This would only give us a json schema validation check on insert/update 
 * but none of the structural schema benefits of the relational counterpart.
 * 
 * @author Joram Barrez
 */
public class MongoProcessSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoProcessSchemaManager.class);
    
    public static final String COLLECTION_PROPERTIES = "flowableProperties";
    
    public static final String SCHEMA_VERSION_PROPERTY = "schema.version";
    
    protected String randomId = UUID.randomUUID().toString();
    
    @Override
    public void schemaCreate() {
        
        MongoDbProcessEngineConfiguration engineConfiguration = getEngineConfiguration();
        String currentSchemaVersion = getVersion(engineConfiguration.getMongoDatabase());
        
        if (currentSchemaVersion == null) {
            try {
                waitForLock(engineConfiguration);
                
                // Need to recheck version, other engine can have come in the meantime
                currentSchemaVersion = getVersion(engineConfiguration.getMongoDatabase());
                if (currentSchemaVersion == null) {
                    initializeDefaultCollectionsAndIndices(engineConfiguration);
                    initSchemaVersionProperty(engineConfiguration);
                }
                
            } finally {
                releaseLock(engineConfiguration);
            }
            
        } else {
            throw new FlowableException("Cannot create collections and indices: previous version already exists");
        }
    }

    protected void initializeDefaultCollectionsAndIndices(MongoDbProcessEngineConfiguration engineConfiguration) {
        initDefaultCollections(engineConfiguration);
        initDefaultIndices(engineConfiguration);
    }

    protected void initDefaultCollections(MongoDbProcessEngineConfiguration engineConfiguration) {
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
        }
    }

    protected void initDefaultIndices(MongoDbProcessEngineConfiguration engineConfiguration) {
        MongoDatabase mongoDatabase = engineConfiguration.getMongoDatabase();
        
        mongoDatabase.getCollection(MongoDbResourceDataManager.COLLECTION_BYTE_ARRAY).createIndex(new Document("deploymentId", 1));
        
        mongoDatabase.getCollection(MongoDbProcessDefinitionDataManager.COLLECTION_PROCESS_DEFINITIONS).createIndex(new Document("deploymentId", 1));
        
        mongoDatabase.getCollection(MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION).createIndex(new Document("configuration", 1));
        
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("businessKey", 1));
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("parentId", 1));
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("superExecutionId", 1));
        mongoDatabase.getCollection(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS).createIndex(new Document("rootProcessInstanceId", 1));
        
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document("createTime", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document().append("subScopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbTaskDataManager.COLLECTION_TASKS).createIndex(new Document().append("scopeDefinitionId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document("taskId", 1));
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES).createIndex(new Document().append("subScopeId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document("taskId", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document("userId", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document("groupId", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document().append("subScopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS).createIndex(new Document().append("scopeDefinitionId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbJobDataManager.COLLECTION_JOBS).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbJobDataManager.COLLECTION_JOBS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbJobDataManager.COLLECTION_JOBS).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbJobDataManager.COLLECTION_JOBS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbJobDataManager.COLLECTION_JOBS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES).createIndex(new Document("startTime", 1));
        mongoDatabase.getCollection(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES).createIndex(new Document("endTime", 1));
        mongoDatabase.getCollection(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES).createIndex(new Document("processDefinitionId", 1));
        
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document("userId", 1));
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document("groupId", 1));
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document("taskId", 1));
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document().append("scopeDefinitionId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES).createIndex(new Document("businessKey", 1));
        mongoDatabase.getCollection(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES).createIndex(new Document("startTime", 1));
        mongoDatabase.getCollection(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES).createIndex(new Document("endTime", 1));
        
        mongoDatabase.getCollection(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES).createIndex(new Document("processDefinitionId", 1));
        mongoDatabase.getCollection(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES).createIndex(new Document().append("subScopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES).createIndex(new Document().append("scopeDefinitionId", 1).append("scopeType", 1));
        
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document().append("name", 1).append("typeName", 1));
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document().append("scopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document().append("subScopeId", 1).append("scopeType", 1));
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document("processInstanceId", 1));
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document("executionId", 1));
        mongoDatabase.getCollection(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES).createIndex(new Document("taskId", 1));
    }
    
    protected void initSchemaVersionProperty(MongoDbProcessEngineConfiguration engineConfiguration) {
        MongoCollection<Document> propertiesCollection = getPropertiesCollection(engineConfiguration);
        propertiesCollection.insertOne(new Document()
                    .append("name", SCHEMA_VERSION_PROPERTY)
                    .append("value", FlowableVersions.CURRENT_VERSION));
    }
    
    protected void waitForLock(MongoDbProcessEngineConfiguration engineConfiguration) {
        boolean acquired = acquireLock(engineConfiguration);
        if (!acquired) {
            long timeout = new Date().getTime() + (5 * 60 * 1000); // TODO: make configurable
            while (!acquired && new Date().getTime() < timeout) {
              acquired = acquireLock(engineConfiguration);
              if (!acquired) {
                LOGGER.info("Waiting for lock....");
                try {
                  Thread.sleep(1000); // TODO: make configurable
                } catch (InterruptedException e) {
                    
                }
              }
            }
            throw new FlowableException("Could not acquire lock within timeout");
          }
    }
    
    protected boolean acquireLock(MongoDbProcessEngineConfiguration engineConfiguration) {
        try {
            // Try to write a random ID to the lock document. There is a unique index on the name, so there can be only one.
            // If the duplicate key exception is thrown, another engine has acquired the lock first.
            MongoCollection<Document> propertiesCollection = getPropertiesCollection(engineConfiguration);
            propertiesCollection.insertOne(new Document().append("name", "lock").append("value", randomId));

            // Even if the write was successful, double-check the value of the written lock
            FindIterable<Document> lockDocumentIterable = propertiesCollection.find(new Document().append("name", "lock"));
            if (lockDocumentIterable != null) {
                Document lockDocument = lockDocumentIterable.first();
                if (lockDocument != null) {
                    return randomId.equals(lockDocument.getString("value"));
                }
            }
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
        }
        return true;
    }
    
    protected void releaseLock(MongoDbProcessEngineConfiguration engineConfiguration) {
        MongoCollection<Document> propertiesCollection = getPropertiesCollection(engineConfiguration);
        if (propertiesCollection != null) {
            propertiesCollection.findOneAndDelete(new Document().append("name", "lock").append("value", randomId));
        }
    }

    protected  MongoCollection<Document> getPropertiesCollection(MongoDbProcessEngineConfiguration engineConfiguration) {
        if (!propertiesCollectionExists(engineConfiguration.getMongoDatabase())) {
            MongoCollection<Document> propertiesCollection = null;
            try {
                engineConfiguration.getMongoDatabase().createCollection(COLLECTION_PROPERTIES);
                propertiesCollection = engineConfiguration.getMongoDatabase().getCollection(COLLECTION_PROPERTIES);
                propertiesCollection.createIndex(new Document("name", 1), new IndexOptions().unique(true).name("flw_schema_version_idx"));
            } catch (Exception e) { 
                // ignore
            }
            return propertiesCollection;
            
        } else  {
            return engineConfiguration.getMongoDatabase().getCollection(COLLECTION_PROPERTIES);
            
        }
    }

    @Override
    public void schemaDrop() {
        LOGGER.info("Dropping all MongoDB collections in the database");
        MongoDatabase mongoDatabase = getEngineConfiguration().getMongoDatabase();
        List<String> existingCollections = getExistingCollections(mongoDatabase);
        for (String collectionName : getAllCollectionNames()) {
            if (existingCollections.contains(collectionName)) {
                MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
                if (collection != null) {
                    collection.drop();
                }
            }
        }
    }

    @Override
    public String schemaUpdate() {
        
        MongoDbProcessEngineConfiguration engineConfiguration = getEngineConfiguration();
        String currentSchemaVersion = getVersion(engineConfiguration.getMongoDatabase());
        
        if (currentSchemaVersion == null) {
            schemaCreate();
            
        } else if (!FlowableVersions.CURRENT_VERSION.equals(currentSchemaVersion)) {
            try {
                waitForLock(engineConfiguration);
                currentSchemaVersion = getVersion(engineConfiguration.getMongoDatabase());
                if (!FlowableVersions.CURRENT_VERSION.equals(currentSchemaVersion)) {
                    // TODO in future release (if needed): loop from current version to latest version and apply updates programmatically
                    // Note that the wait and release of the lock also need to happen in this case
                }
                
            } finally {
                releaseLock(engineConfiguration);
            }
            
        } else {
            LOGGER.info("Schema is up to date");
            
        }
        
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        String version = getVersion(getEngineConfiguration().getMongoDatabase());
        if (!FlowableVersions.CURRENT_VERSION.equals(version)) {
            throw new FlowableException("Invalid version. Current schema version is " + version);
        }
    }
    
    protected String getVersion(MongoDatabase mongoDatabase) {
        boolean propertiesCollectionExists = propertiesCollectionExists(mongoDatabase);
        if (propertiesCollectionExists) {
           return getSchemaVersionValue(mongoDatabase);
        }
        return null;
    }

    protected boolean propertiesCollectionExists(MongoDatabase mongoDatabase) {
        MongoIterable<String> collectionNames = mongoDatabase.listCollectionNames();
        if (collectionNames != null) {
            for (String collectionName : collectionNames) {
                if (COLLECTION_PROPERTIES.equals(collectionName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected List<String> getExistingCollections(MongoDatabase mongoDatabase) {
        List<String> result = new ArrayList<>();
        MongoIterable<String> collectionNames = mongoDatabase.listCollectionNames();
        if (collectionNames != null) {
            for (String collectionName : collectionNames) {
                result.add(collectionName);
            }
        }
        return result;
    }
    
    protected String getSchemaVersionValue(MongoDatabase mongoDatabase) {
        MongoCollection<Document> propertiesCollection = mongoDatabase.getCollection(COLLECTION_PROPERTIES);
        FindIterable<Document> schemaVersionDocument = propertiesCollection.find(Filters.eq("name", SCHEMA_VERSION_PROPERTY));
        if (schemaVersionDocument != null) {
            Document document = schemaVersionDocument.first();
            if (document != null) {
                return document.getString("value");
            }
        }
        return null;
    }
    
    protected Collection<String> getAllCollectionNames() {
        MongoDbSessionFactory mongoDbSessionFactory = (MongoDbSessionFactory) getEngineConfiguration().getSessionFactories().get(MongoDbSession.class);
        Set<String> collectionNames = new HashSet<>();
        collectionNames.addAll(mongoDbSessionFactory.getCollectionNames());
        collectionNames.add(COLLECTION_PROPERTIES);
        return collectionNames;
    }
    
    protected MongoDbProcessEngineConfiguration getEngineConfiguration() {
        MongoDbProcessEngineConfiguration engineConfiguration = (MongoDbProcessEngineConfiguration) CommandContextUtil.getProcessEngineConfiguration();
        return engineConfiguration;
    }

}
