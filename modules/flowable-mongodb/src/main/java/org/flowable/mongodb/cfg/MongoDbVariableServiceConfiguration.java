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

import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.flowable.mongodb.persistence.manager.MongoDbHistoricVariableInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbVariableInstanceDataManager;
import org.flowable.variable.service.VariableServiceConfiguration;

/**
 * @author Joram Barrez
 */
public class MongoDbVariableServiceConfiguration extends VariableServiceConfiguration {
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    
    @Override
    public void initDataManagers() {
        MongoDbVariableInstanceDataManager mongoDbVariableInstanceDataManager = new MongoDbVariableInstanceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbVariableInstanceDataManager.COLLECTION_VARIABLES, mongoDbVariableInstanceDataManager);
        this.variableInstanceDataManager = mongoDbVariableInstanceDataManager;
        
        MongoDbHistoricVariableInstanceDataManager mongoDbHistoricVariableInstanceDataManager = new MongoDbHistoricVariableInstanceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbHistoricVariableInstanceDataManager.COLLECTION_HISTORIC_VARIABLE_INSTANCES, mongoDbHistoricVariableInstanceDataManager);
        this.historicVariableInstanceDataManager = mongoDbHistoricVariableInstanceDataManager;
        
    }

    public MongoDbSessionFactory getMongoDbSessionFactory() {
        return mongoDbSessionFactory;
    }

    public void setMongoDbSessionFactory(MongoDbSessionFactory mongoDbSessionFactory) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
    }
}
