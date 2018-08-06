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
import org.flowable.mongodb.persistence.manager.MongoDbJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDbTimerJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoDeadLetterJobDataManager;
import org.flowable.mongodb.persistence.manager.MongoSuspendedJobDataManager;

/**
 * @author Joram Barrez
 */
public class MongoDbJobServiceConfiguration extends JobServiceConfiguration {
    
    @Override
    public void initDataManagers() {
        // TODO: other data managers
        if (jobDataManager == null) {
            jobDataManager = new MongoDbJobDataManager();
        }
        if (timerJobDataManager == null) {
            timerJobDataManager = new MongoDbTimerJobDataManager();
        }
        if (suspendedJobDataManager == null) {
            suspendedJobDataManager = new MongoSuspendedJobDataManager();
        }
        if (deadLetterJobDataManager == null) {
            deadLetterJobDataManager = new MongoDeadLetterJobDataManager();
        }
    }

}
