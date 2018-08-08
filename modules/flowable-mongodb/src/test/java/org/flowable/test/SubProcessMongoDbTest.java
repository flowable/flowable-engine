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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ServerAddress;

/**
 * @author Joram Barrez
 */
public class SubProcessMongoDbTest {
    
    private MongoDbProcessEngineConfiguration mongoDbProcessEngineConfiguration;
    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;
    
    @BeforeEach
    public void setup() {
        this.mongoDbProcessEngineConfiguration = (MongoDbProcessEngineConfiguration) new MongoDbProcessEngineConfiguration()
                .setServerAddresses(Arrays.asList(new ServerAddress("localhost", 27017), new ServerAddress("localhost", 27018), new ServerAddress("localhost", 27019)))
                .setDisableIdmEngine(true)
                .setHistoryLevel(HistoryLevel.NONE);
        this.processEngine = mongoDbProcessEngineConfiguration.buildProcessEngine();
        this.repositoryService = processEngine.getRepositoryService();
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
    }
    
    @AfterEach
    public void cleanup() {
        mongoDbProcessEngineConfiguration.getMongoDatabase().drop();
    }
    
    @Test
    public void testNestedSubProcess() {
        repositoryService.createDeployment().addClasspathResource("nestedSubProcess.bpmn20.xml").deploy();
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSubprocesses");
        
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(5, executions.size());
        
        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        
        taskService.complete(tasks.get(0).getId());
        
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(5, executions.size());
        
        taskService.complete(tasks.get(1).getId());
        
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(0, executions.size());
        
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

}
