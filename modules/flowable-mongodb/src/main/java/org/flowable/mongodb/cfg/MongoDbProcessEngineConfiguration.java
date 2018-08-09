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
package org.flowable.mongodb.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.persistence.GenericManagerFactory;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.flowable.mongodb.persistence.manager.MongoDbDeploymentDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbEventSubscriptionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbExecutionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbIdentityLinkDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionInfoDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbResourceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTaskDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTimerJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbVariableInstanceDataManager;
import org.flowable.mongodb.transaction.MongoDbTransactionContextFactory;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.variable.service.VariableServiceConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

/**
 * @author Joram Barrez
 */
public class MongoDbProcessEngineConfiguration extends ProcessEngineConfigurationImpl {
    
    protected List<ServerAddress> serverAddresses = new ArrayList<>();
    protected String databaseName = "flowable";
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    
    protected List<String> collectionNames;
    
    public MongoDbProcessEngineConfiguration() {
        this.usingRelationalDatabase = false;
        
        this.validateFlowable5EntitiesEnabled = false;
        this.performanceSettings.setValidateExecutionRelationshipCountConfigOnBoot(false);
        this.performanceSettings.setValidateTaskRelationshipCountConfigOnBoot(false);
        
        this.idGenerator = new StrongUuidGenerator(); 
    }
    
    @Override
    public void initNonRelationalDataSource() {
        if (this.mongoClient == null) {
            this.mongoClient = new com.mongodb.MongoClient(serverAddresses);  
        }
        
        //TODO Schema mgmt
        
        if (this.mongoDatabase == null)  {
            this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
            
            // TODO needs to be extracted
            
            // Collections can't be created in a transaction (see https://docs.mongodb.com/manual/core/transactions/)
            
            Set<String> collections = new HashSet<>();
            MongoIterable<String> collectionNames = this.mongoDatabase.listCollectionNames();
            if (collectionNames != null) {
                for (String collectionName : collectionNames) {
                    collections.add(collectionName);
                }
            }
            
            this.collectionNames = Arrays.asList(
                    MongoDbDeploymentDataManager.COLLECTION_DEPLOYMENT,
                    MongoDbProcessDefinitionDataManager.COLLECTION_PROCESS_DEFINITIONS,
                    MongoDbResourceDataManager.COLLECTION_BYTE_ARRAY,
                    MongoDbExecutionDataManager.COLLECTION_EXECUTIONS,
                    MongoDbProcessDefinitionInfoDataManager.COLLECTION_PROCESS_DEFINITION_INFO,
                    MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION,
                    MongoDbTaskDataManager.COLLECTION_TASKS,
                    MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS,
                    MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES,
                    MongoDbJobDataManager.COLLECTION_JOBS,
                    MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS);
            for (String name : this.collectionNames) {
                if (!collections.contains(name)) {
                    this.mongoDatabase.createCollection(name);
                }
            };
            
        }
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
//        return new MongoDbTransactionInterceptor(mongoClient);
    }
    
    @Override
    public void initTransactionContextFactory() {
        if (transactionContextFactory == null) {
            transactionContextFactory = new MongoDbTransactionContextFactory();
        }
    }
    
    @Override
    public void initDataManagers() {
        MongoDbDeploymentDataManager mongoDeploymentDataManager = new MongoDbDeploymentDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbDeploymentDataManager.COLLECTION_DEPLOYMENT, mongoDeploymentDataManager);
        this.deploymentDataManager = mongoDeploymentDataManager;
        
        MongoDbResourceDataManager mongoDbResourceDataManager = new MongoDbResourceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbResourceDataManager.COLLECTION_BYTE_ARRAY, mongoDbResourceDataManager);
        this.resourceDataManager = mongoDbResourceDataManager;
        
        MongoDbProcessDefinitionDataManager mongoDbProcessDefinitionDataManager = new MongoDbProcessDefinitionDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbProcessDefinitionDataManager.COLLECTION_PROCESS_DEFINITIONS, mongoDbProcessDefinitionDataManager);
        this.processDefinitionDataManager = mongoDbProcessDefinitionDataManager;
        
        MongoDbExecutionDataManager mongoDbExecutionDataManager = new MongoDbExecutionDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbExecutionDataManager.COLLECTION_EXECUTIONS, mongoDbExecutionDataManager);
        this.executionDataManager = mongoDbExecutionDataManager;
        
        MongoDbProcessDefinitionInfoDataManager mongoDbProcessDefinitionInfoDataManager = new MongoDbProcessDefinitionInfoDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbProcessDefinitionInfoDataManager.COLLECTION_PROCESS_DEFINITION_INFO, mongoDbProcessDefinitionInfoDataManager);
        this.processDefinitionInfoDataManager = mongoDbProcessDefinitionInfoDataManager;
        
        MongoDbEventSubscriptionDataManager mongoDbEventSubscriptionDataManager = new MongoDbEventSubscriptionDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION, mongoDbEventSubscriptionDataManager);
        this.eventSubscriptionDataManager = mongoDbEventSubscriptionDataManager;
    }
    
    @Override
    protected JobServiceConfiguration instantiateJobServiceConfiguration() {
        return new MongoDbJobServiceConfiguration();
    }
    
    @Override
    protected TaskServiceConfiguration instantiateTaskServiceConfiguration() {
        return new MongoDbTaskServiceConfiguration();
    }
    
    @Override
    protected IdentityLinkServiceConfiguration instantiateIdentityLinkServiceConfiguration() {
        return new MongoDbIdentityLinkServiceConfiguration();
    }
    
    @Override
    protected VariableServiceConfiguration instantiateVariableServiceConfiguration() {
        return new MongoDbVariableServiceConfiguration();
    }
    
    @Override
    public void initSessionFactories() {
        
        if (this.customSessionFactories == null) {
            this.customSessionFactories = new ArrayList<>();
        }
        
        this.customSessionFactories.add(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
        
        initMongoDbSessionFactory();
        this.customSessionFactories.add(mongoDbSessionFactory);    
        
        super.initSessionFactories();
    }
    
    public void initMongoDbSessionFactory() {
        if (this.mongoDbSessionFactory == null) {
            this.mongoDbSessionFactory = new MongoDbSessionFactory(mongoClient, mongoDatabase);
        }
    }
    
    public List<ServerAddress> getServerAddresses() {
        return serverAddresses;
    }

    public MongoDbProcessEngineConfiguration setServerAddresses(List<ServerAddress> serverAddresses) {
        this.serverAddresses = serverAddresses;
        return this;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public MongoDbProcessEngineConfiguration setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDbProcessEngineConfiguration setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        return this;
    }
    
    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public List<String> getCollectionNames() {
        return collectionNames;
    }

    public void setCollectionNames(List<String> collectionNames) {
        this.collectionNames = collectionNames;
    }
    
}
