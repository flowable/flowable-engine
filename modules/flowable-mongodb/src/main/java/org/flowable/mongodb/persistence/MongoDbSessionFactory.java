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

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.mongodb.persistence.manager.AbstractMongoDbDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbDeploymentDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbExecutionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbIdentityLinkDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbResourceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTaskDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbVariableInstanceDataManager;
import org.flowable.mongodb.persistence.mapper.DeploymentEntityMapper;
import org.flowable.mongodb.persistence.mapper.ExecutionEntityMapper;
import org.flowable.mongodb.persistence.mapper.IdentityLinkEntityMapper;
import org.flowable.mongodb.persistence.mapper.ProcessDefinitionEntityMapper;
import org.flowable.mongodb.persistence.mapper.ResourceEntityMapper;
import org.flowable.mongodb.persistence.mapper.TaskEntityMapper;
import org.flowable.mongodb.persistence.mapper.VariableInstanceEntityMapper;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * @author Joram Barrez
 */ 
public class MongoDbSessionFactory implements SessionFactory {

    protected MongoClient mongoClient;
    protected MongoDatabase mongoDatabase;
    
    protected Map<Class<? extends Entity>, String> collections = new HashMap<>();
    protected Map<String, EntityMapper<? extends Entity>> collectionToMapper = new HashMap<>();
    protected Map<String, Class<? extends Entity>> collectionToClass = new HashMap<>();
    protected Map<Class<? extends Entity>, EntityMapper<? extends Entity>> entityMappers = new HashMap<>();
    protected Map<String, AbstractMongoDbDataManager> collectionToDataManager = new HashMap<>();
    
    public MongoDbSessionFactory(MongoClient mongoClient, MongoDatabase mongoDatabase) {
        this.mongoClient = mongoClient;
        this.mongoDatabase = mongoDatabase;
        
        initDefaultMappers();
    }
    
    protected void initDefaultMappers() {
        registerEntityMapper(DeploymentEntityImpl.class, new DeploymentEntityMapper(), MongoDbDeploymentDataManager.COLLECTION_DEPLOYMENT);
        registerEntityMapper(ResourceEntityImpl.class, new ResourceEntityMapper(), MongoDbResourceDataManager.COLLECTION_BYTE_ARRAY);
        registerEntityMapper(ProcessDefinitionEntityImpl.class, new ProcessDefinitionEntityMapper(), MongoDbProcessDefinitionDataManager.COLLECTION_PROCESS_DEFINITIONS);
        registerEntityMapper(ExecutionEntityImpl.class, new ExecutionEntityMapper(), MongoDbExecutionDataManager.COLLECTION_EXECUTIONS);
        registerEntityMapper(IdentityLinkEntityImpl.class, new IdentityLinkEntityMapper(), MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS);
        registerEntityMapper(TaskEntityImpl.class, new TaskEntityMapper(), MongoDbTaskDataManager.COLLECTION_TASKS);
        registerEntityMapper(VariableInstanceEntityImpl.class, new VariableInstanceEntityMapper(), MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES);
    }

    @Override
    public Class<?> getSessionType() {
        return MongoDbSession.class;
    }

    @Override
    public Session openSession(CommandContext commandContext) {
        return new MongoDbSession(this, mongoClient, mongoDatabase, Context.getCommandContext().getSession(EntityCache.class));
    }
    
    public void registerEntityMapper(Class<? extends Entity> clazz, EntityMapper<? extends Entity> mapper, String collection) {
        entityMappers.put(clazz, mapper);
        collections.put(clazz, collection);
        collectionToClass.put(collection, clazz);
        collectionToMapper.put(collection, mapper);
    }
    
    public void registerDataManager(String collection, AbstractMongoDbDataManager dataManager) {
        collectionToDataManager.put(collection, dataManager);
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
    
    public String getCollectionForEntityClass(Class<? extends Entity> clazz) {
        return collections.get(clazz);
    }

    public Map<Class<? extends Entity>, String> getCollections() {
        return collections;
    }

    public void setCollections(Map<Class<? extends Entity>, String> collections) {
        this.collections = collections;
    }
    
    public EntityMapper<? extends Entity> getMapperForCollection(String collection) {
        return collectionToMapper.get(collection);
    }

    public Map<String, EntityMapper<? extends Entity>> getCollectionToMapper() {
        return collectionToMapper;
    }

    public void setCollectionToMapper(Map<String, EntityMapper<? extends Entity>> collectionToMapper) {
        this.collectionToMapper = collectionToMapper;
    }
    
    public Class<? extends Entity> getClassForCollection(String collection) {
        return collectionToClass.get(collection);
    }
    
    public AbstractMongoDbDataManager getDataManagerForCollection(String collection) {
        return collectionToDataManager.get(collection);
    }
    
    public Map<String, Class<? extends Entity>> getCollectionToClass() {
        return collectionToClass;
    }

    public void setCollectionToClass(Map<String, Class<? extends Entity>> collectionToClass) {
        this.collectionToClass = collectionToClass;
    }

    public EntityMapper<? extends Entity> getMapperForEntityClass(Class<? extends Entity> clazz) {
        return entityMappers.get(clazz);
    }

    public Map<Class<? extends Entity>, EntityMapper<? extends Entity>> getEntityMappers() {
        return entityMappers;
    }

    public void setEntityMappers(Map<Class<? extends Entity>, EntityMapper<? extends Entity>> entityMappers) {
        this.entityMappers = entityMappers;
    }
    
}
