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
package org.flowable.test;

import java.util.Arrays;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ServerAddress;

/**
 * @author Tijs Rademakers
 */
public class CleanMongoDbTest {
    
    protected MongoDbProcessEngineConfiguration mongoDbProcessEngineConfiguration;
    protected ProcessEngine processEngine;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected HistoryService historyService;
    protected ManagementService managementService;
    
    @BeforeEach
    public void setup() {
        this.mongoDbProcessEngineConfiguration = (MongoDbProcessEngineConfiguration) new MongoDbProcessEngineConfiguration()
                .setServerAddresses(Arrays.asList(new ServerAddress("localhost", 27017), new ServerAddress("localhost", 27018), new ServerAddress("localhost", 27019)))
                .setDisableIdmEngine(true)
                .setHistoryLevel(HistoryLevel.AUDIT);
        
        DefaultAsyncJobExecutor asyncJobExecutor = new DefaultAsyncJobExecutor();
        asyncJobExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(1000);
        asyncJobExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(1000);
        mongoDbProcessEngineConfiguration.setAsyncExecutor(asyncJobExecutor);
        mongoDbProcessEngineConfiguration.setAsyncFailedJobWaitTime(1);
        
        this.processEngine = mongoDbProcessEngineConfiguration.buildProcessEngine();
        this.repositoryService = processEngine.getRepositoryService();
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
        this.historyService = processEngine.getHistoryService();
        this.managementService = processEngine.getManagementService();
    }
    
    @AfterEach
    public void cleanup() {
        mongoDbProcessEngineConfiguration.getMongoDatabase().drop();
    }
    
    @Test
    public void fakeTest() {
        
    }

}
