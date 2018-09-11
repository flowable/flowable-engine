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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.mongodb.persistence.manager.MongoDbCommentDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbDeploymentDataManager;
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
import org.flowable.mongodb.persistence.manager.MongoDbProcessDefinitionInfoDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbResourceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTaskDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTimerJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbVariableInstanceDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        List<String> collectionNames = Arrays.asList(
                MongoDbDeploymentDataManager.COLLECTION_DEPLOYMENT,
                MongoDbProcessDefinitionDataManager.COLLECTION_PROCESS_DEFINITIONS,
                MongoDbResourceDataManager.COLLECTION_BYTE_ARRAY,
                MongoDbExecutionDataManager.COLLECTION_EXECUTIONS,
                MongoDbProcessDefinitionInfoDataManager.COLLECTION_PROCESS_DEFINITION_INFO,
                MongoDbEventSubscriptionDataManager.COLLECTION_EVENT_SUBSCRIPTION,
                MongoDbHistoricProcessInstanceDataManager.COLLECTION_HISTORIC_PROCESS_INSTANCES,
                MongoDbHistoricActivityInstanceDataManager.COLLECTION_HISTORIC_ACTIVITY_INSTANCES,
                MongoDbCommentDataManager.COLLECTION_COMMENTS,
                MongoDbTaskDataManager.COLLECTION_TASKS,
                MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES,
                MongoDbIdentityLinkDataManager.COLLECTION_IDENTITY_LINKS,
                MongoDbHistoricIdentityLinkDataManager.COLLECTION_HISTORIC_IDENTITY_LINKS,
                MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES,
                MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES,
                MongoDbJobDataManager.COLLECTION_JOBS,
                MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS);
        
        for (String name : collectionNames) {
            if (!collections.contains(name)) {
                engineConfiguration.getMongoDatabase().createCollection(name);
            }
        };
    }

    @Override
    public void schemaDrop() {
        LOGGER.info("Dropping all MongoDB collections in the database");
        getEngineConfiguration().getMongoDatabase().drop();
    }

    @Override
    public String schemaUpdate() {
        schemaCreate();
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        
    }
    
    protected MongoDbProcessEngineConfiguration getEngineConfiguration() {
        MongoDbProcessEngineConfiguration engineConfiguration = (MongoDbProcessEngineConfiguration) CommandContextUtil.getProcessEngineConfiguration();
        return engineConfiguration;
    }

}
