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
package org.flowable.crystalball.simulator.impl.replay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.crystalball.simulator.ReplaySimulationRun;
import org.flowable.crystalball.simulator.SimulationDebugger;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.delegate.UserTaskExecutionListener;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.crystalball.simulator.delegate.event.impl.InMemoryRecordFlowableEventListener;
import org.flowable.crystalball.simulator.delegate.event.impl.ProcessInstanceCreateTransformer;
import org.flowable.crystalball.simulator.delegate.event.impl.UserTaskCompleteTransformer;
import org.flowable.crystalball.simulator.impl.StartReplayProcessEventHandler;
import org.flowable.crystalball.simulator.impl.bpmn.parser.handler.AddListenerUserTaskParseHandler;
import org.flowable.crystalball.simulator.impl.playback.PlaybackUserTaskCompleteEventHandler;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
public class ReplayRunTest {

    // Process instance start event
    private static final String PROCESS_INSTANCE_START_EVENT_TYPE = "PROCESS_INSTANCE_START";
    private static final String PROCESS_DEFINITION_ID_KEY = "processDefinitionId";
    private static final String VARIABLES_KEY = "variables";
    // User task completed event
    private static final String USER_TASK_COMPLETED_EVENT_TYPE = "USER_TASK_COMPLETED";

    private static final String USERTASK_PROCESS = "oneTaskProcess";
    private static final String BUSINESS_KEY = "testBusinessKey";
    private static final String TEST_VALUE = "TestValue";
    private static final String TEST_VARIABLE = "testVariable";

    protected static InMemoryRecordFlowableEventListener listener = new InMemoryRecordFlowableEventListener(getTransformers());

    private static final String THE_USERTASK_PROCESS = "org/flowable/crystalball/simulator/impl/playback/PlaybackProcessStartTest.testUserTask.bpmn20.xml";

    @Test
    public void testProcessInstanceStartEvents() throws Exception {
        ProcessEngineImpl processEngine = initProcessEngine();

        TaskService taskService = processEngine.getTaskService();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables = new HashMap<>();
        variables.put(TEST_VARIABLE, TEST_VALUE);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(USERTASK_PROCESS, BUSINESS_KEY, variables);

        Task task = taskService.createTaskQuery().taskDefinitionKey("userTask").singleResult();
        TimeUnit.MILLISECONDS.sleep(50);
        taskService.complete(task.getId());

        final SimulationDebugger simRun = new ReplaySimulationRun(processEngine, getReplayHandlers(processInstance.getId()));

        simRun.init(new NoExecutionVariableScope());

        // original process is finished - there should not be any running process instance/task
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(USERTASK_PROCESS).count()).isZero();
        assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isZero();

        simRun.step();

        // replay process was started
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(USERTASK_PROCESS).count()).isEqualTo(1);
        // there should be one task
        assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isEqualTo(1);

        simRun.step();

        // userTask was completed - replay process was finished
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(USERTASK_PROCESS).count()).isZero();
        assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask").count()).isZero();

        simRun.close();
        processEngine.close();
        ProcessEngines.destroy();
    }

    private ProcessEngineImpl initProcessEngine() {
        ProcessEngineConfigurationImpl configuration = getProcessEngineConfiguration();
        ProcessEngineImpl processEngine = (ProcessEngineImpl) configuration.buildProcessEngine();

        processEngine.getRepositoryService().createDeployment().addClasspathResource(THE_USERTASK_PROCESS).deploy();
        return processEngine;
    }

    private ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        ProcessEngineConfigurationImpl configuration = new org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration();
        configuration.setHistory("full").setDatabaseSchemaUpdate("true");
        configuration.setCustomDefaultBpmnParseHandlers(
                Collections.singletonList(
                        new AddListenerUserTaskParseHandler(TaskListener.EVENTNAME_CREATE,
                                new UserTaskExecutionListener(USER_TASK_COMPLETED_EVENT_TYPE, USER_TASK_COMPLETED_EVENT_TYPE, listener.getSimulationEvents()))));
        configuration.setEventListeners(Collections.singletonList(listener));
        return configuration;
    }

    private static List<Function<FlowableEvent, SimulationEvent>> getTransformers() {
        List<Function<FlowableEvent, SimulationEvent>> transformers = new ArrayList<>();
        transformers.add(new ProcessInstanceCreateTransformer(PROCESS_INSTANCE_START_EVENT_TYPE, PROCESS_DEFINITION_ID_KEY, BUSINESS_KEY, VARIABLES_KEY));
        transformers.add(new UserTaskCompleteTransformer(USER_TASK_COMPLETED_EVENT_TYPE));
        return transformers;
    }

    public static Map<String, SimulationEventHandler> getReplayHandlers(String processInstanceId) {
        Map<String, SimulationEventHandler> handlers = new HashMap<>();
        handlers.put(PROCESS_INSTANCE_START_EVENT_TYPE,
                new StartReplayProcessEventHandler(processInstanceId, PROCESS_INSTANCE_START_EVENT_TYPE, PROCESS_INSTANCE_START_EVENT_TYPE, listener.getSimulationEvents(), PROCESS_DEFINITION_ID_KEY, BUSINESS_KEY, VARIABLES_KEY));
        handlers.put(USER_TASK_COMPLETED_EVENT_TYPE, new PlaybackUserTaskCompleteEventHandler());
        return handlers;
    }
}
