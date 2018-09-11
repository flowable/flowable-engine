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
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.persistence.GenericManagerFactory;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.impl.SchemaOperationsProcessEngineBuild;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.flowable.mongodb.persistence.manager.MongoDbCommentDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbDeploymentDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbEventSubscriptionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbExecutionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricActivityInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricProcessInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionInfoDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbResourceDataManager;
import org.flowable.mongodb.schema.MongoProcessSchemaManager;
import org.flowable.mongodb.transaction.MongoDbTransactionContextFactory;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.variable.service.VariableServiceConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

/**
 * @author Joram Barrez
 */
public class MongoDbProcessEngineConfiguration extends ProcessEngineConfigurationImpl {
    
    protected List<ServerAddress> serverAddresses = new ArrayList<>();
    protected String databaseName = "flowable";
    protected MongoClientOptions mongoClientOptions;
    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    protected MongoProcessSchemaManager processSchemaManager;
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    
    
    public MongoDbProcessEngineConfiguration() {
        this.usingRelationalDatabase = false;
        this.usingSchemaMgmt = true;
        this.databaseSchemaUpdate = DB_SCHEMA_UPDATE_TRUE;
        
        this.validateFlowable5EntitiesEnabled = false;
        this.performanceSettings.setValidateExecutionRelationshipCountConfigOnBoot(false);
        this.performanceSettings.setValidateTaskRelationshipCountConfigOnBoot(false);
        
        this.idGenerator = new StrongUuidGenerator(); 
    }
    
    @Override
    public void initNonRelationalDataSource() {
        if (this.mongoClientOptions == null) {
            this.mongoClientOptions = MongoClientOptions.builder().build();
        }
        if (this.mongoClient == null) {
            this.mongoClient = new com.mongodb.MongoClient(serverAddresses, mongoClientOptions);  
        }
        
        if (this.mongoDatabase == null)  {
            this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
        }
    }
    
    @Override
    public void initSchemaManager() {
        this.schemaManager = new MongoProcessSchemaManager();
    }
    
    public void initSchemaManagementCommand() {
        // Impl note: the schemaMgmtCmd of the regular impl is reused, as it will delegate to the MongoProcessSchemaManager class
        if (schemaManagementCmd == null) {
            this.schemaManagementCmd = new SchemaOperationsProcessEngineBuild();
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
        
        MongoDbHistoricProcessInstanceDataManager mongoDbHistoricProcessInstanceDataManager = new MongoDbHistoricProcessInstanceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES, mongoDbHistoricProcessInstanceDataManager);
        this.historicProcessInstanceDataManager = mongoDbHistoricProcessInstanceDataManager;
        
        MongoDbHistoricActivityInstanceDataManager mongoDbHistoricActivityInstanceDataManager = new MongoDbHistoricActivityInstanceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES, mongoDbHistoricActivityInstanceDataManager);
        this.historicActivityInstanceDataManager = mongoDbHistoricActivityInstanceDataManager;
        
        MongoDbCommentDataManager mongoDbCommentDataManager = new MongoDbCommentDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbCommentDataManager.COLLECTION_COMMENTS, mongoDbCommentDataManager);
        this.commentDataManager = mongoDbCommentDataManager;
    }
    
    @Override
    protected JobServiceConfiguration instantiateJobServiceConfiguration() {
        MongoDbJobServiceConfiguration mongoDbJobServiceConfiguration = new MongoDbJobServiceConfiguration();
        mongoDbJobServiceConfiguration.setMongoDbSessionFactory(mongoDbSessionFactory);
        return mongoDbJobServiceConfiguration;
    }
    
    @Override
    protected TaskServiceConfiguration instantiateTaskServiceConfiguration() {
        MongoDbTaskServiceConfiguration mongoDbTaskServiceConfiguration = new MongoDbTaskServiceConfiguration();
        mongoDbTaskServiceConfiguration.setMongoDbSessionFactory(mongoDbSessionFactory);
        return mongoDbTaskServiceConfiguration;
    }
    
    @Override
    protected IdentityLinkServiceConfiguration instantiateIdentityLinkServiceConfiguration() {
        MongoDbIdentityLinkServiceConfiguration mongoDbIdentityLinkServiceConfiguration = new MongoDbIdentityLinkServiceConfiguration();
        mongoDbIdentityLinkServiceConfiguration.setMongoDbSessionFactory(mongoDbSessionFactory);
        return mongoDbIdentityLinkServiceConfiguration;
    }
    
    @Override
    protected VariableServiceConfiguration instantiateVariableServiceConfiguration() {
        MongoDbVariableServiceConfiguration mongoDbVariableServiceConfiguration = new MongoDbVariableServiceConfiguration();
        mongoDbVariableServiceConfiguration.setMongoDbSessionFactory(mongoDbSessionFactory);
        return mongoDbVariableServiceConfiguration;
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
    
    /**
     * server addresses in the form of "host:port, host:port, ..." 
     */
    public MongoDbProcessEngineConfiguration setServerAddresses(String serverAddresses) {
        List<ServerAddress> result = new ArrayList<>();
        
        String[] addresses = serverAddresses.split(",");
        for (String address : addresses) {
            String[] splittedAddress = address.split(":");
            String host = splittedAddress[0].trim();
            int port = Integer.valueOf(splittedAddress[1].trim());
            
            result.add(new ServerAddress(host, port));
        }
        
        setServerAddresses(serverAddresses);
        return this;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public MongoDbProcessEngineConfiguration setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
    
    public MongoClientOptions getMongoClientOptions() {
        return mongoClientOptions;
    }

    public void setMongoClientOptions(MongoClientOptions mongoClientOptions) {
        this.mongoClientOptions = mongoClientOptions;
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

    public MongoProcessSchemaManager getProcessSchemaManager() {
        return processSchemaManager;
    }

    public MongoDbProcessEngineConfiguration setProcessSchemaManager(MongoProcessSchemaManager processSchemaManager) {
        this.processSchemaManager = processSchemaManager;
        return this;
    }
    
}
