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
import org.flowable.mongodb.persistence.manager.MongoDbHistoricTaskInstanceDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTaskDataManager;
import org.flowable.task.service.TaskServiceConfiguration;

/**
 * @author Joram Barrez
 */
public class MongoDbTaskServiceConfiguration extends TaskServiceConfiguration {
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    
    @Override
    public void initDataManagers() {
        MongoDbTaskDataManager mongoDbTaskDataManager = new MongoDbTaskDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbTaskDataManager.COLLECTION_TASKS, mongoDbTaskDataManager);
        this.taskDataManager = mongoDbTaskDataManager;
        
        MongoDbHistoricTaskInstanceDataManager mongoDbHistoricTaskInstanceDataManager = new MongoDbHistoricTaskInstanceDataManager();
        mongoDbSessionFactory.registerDataManager(MongoDbHistoricTaskInstanceDataManager.COLLECTION_HISTORIC_TASK_INSTANCES, mongoDbHistoricTaskInstanceDataManager);
        this.historicTaskInstanceDataManager = mongoDbHistoricTaskInstanceDataManager;
    }

    public MongoDbSessionFactory getMongoDbSessionFactory() {
        return mongoDbSessionFactory;
    }

    public void setMongoDbSessionFactory(MongoDbSessionFactory mongoDbSessionFactory) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
    }
}
