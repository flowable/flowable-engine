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

import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.mongodb.persistence.MongoDbSessionFactory;
import org.flowable.mongodb.persistence.manager.MongoDbJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTimerJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDeadLetterJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoSuspendedJobDataManager;

/**
 * @author Joram Barrez
 */
public class MongoDbJobServiceConfiguration extends JobServiceConfiguration {
    
    protected MongoDbSessionFactory mongoDbSessionFactory;
    
    @Override
    public void initDataManagers() {
        // TODO: other data managers
        if (jobDataManager == null) {
            MongoDbJobDataManager mongoDbJobDataManager = new MongoDbJobDataManager();
            mongoDbSessionFactory.registerDataManager(MongoDbJobDataManager.COLLECTION_JOBS, mongoDbJobDataManager);
            jobDataManager = mongoDbJobDataManager;
        }
        if (timerJobDataManager == null) {
            MongoDbTimerJobDataManager mongoDbTimerJobDataManager = new MongoDbTimerJobDataManager();
            mongoDbSessionFactory.registerDataManager(MongoDbTimerJobDataManager.COLLECTION_TIMER_JOBS, mongoDbTimerJobDataManager);
            timerJobDataManager = mongoDbTimerJobDataManager;
        }
        if (suspendedJobDataManager == null) {
            MongoSuspendedJobDataManager mongoSuspendedJobDataManager = new MongoSuspendedJobDataManager();
            mongoDbSessionFactory.registerDataManager(MongoSuspendedJobDataManager.COLLECTION_SUSPENDED_JOBS, mongoSuspendedJobDataManager);
            suspendedJobDataManager = mongoSuspendedJobDataManager;
        }
        if (deadLetterJobDataManager == null) {
            MongoDeadLetterJobDataManager mongoDeadLetterJobDataManager = new MongoDeadLetterJobDataManager();
            mongoDbSessionFactory.registerDataManager(MongoDeadLetterJobDataManager.COLLECTION_DEADLETTER_JOBS, mongoDeadLetterJobDataManager);
            deadLetterJobDataManager = mongoDeadLetterJobDataManager;
        }
    }

    public MongoDbSessionFactory getMongoDbSessionFactory() {
        return mongoDbSessionFactory;
    }

    public void setMongoDbSessionFactory(MongoDbSessionFactory mongoDbSessionFactory) {
        this.mongoDbSessionFactory = mongoDbSessionFactory;
    }
}
