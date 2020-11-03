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
package org.flowable.crystalball.simulator.impl.playback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DefaultClockImpl;
import org.flowable.crystalball.simulator.SimpleEventCalendarFactory;
import org.flowable.crystalball.simulator.SimpleSimulationRun;
import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventComparator;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.crystalball.simulator.delegate.event.impl.DeploymentCreateTransformer;
import org.flowable.crystalball.simulator.delegate.event.impl.InMemoryRecordFlowableEventListener;
import org.flowable.crystalball.simulator.delegate.event.impl.ProcessInstanceCreateTransformer;
import org.flowable.crystalball.simulator.delegate.event.impl.UserTaskCompleteTransformer;
import org.flowable.crystalball.simulator.impl.DeployResourcesEventHandler;
import org.flowable.crystalball.simulator.impl.EventRecorderTestUtils;
import org.flowable.crystalball.simulator.impl.RecordableProcessEngineFactory;
import org.flowable.crystalball.simulator.impl.SimulationProcessEngineFactory;
import org.flowable.crystalball.simulator.impl.StartProcessByIdEventHandler;
import org.flowable.crystalball.simulator.impl.clock.DefaultClockFactory;
import org.flowable.crystalball.simulator.impl.clock.ThreadLocalClock;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
public class PlaybackRunTest {
    // deployment created
    private static final String DEPLOYMENT_CREATED_EVENT_TYPE = "DEPLOYMENT_CREATED_EVENT";
    private static final String DEPLOYMENT_RESOURCES_KEY = "deploymentResources";

    // Process instance start event
    private static final String PROCESS_INSTANCE_START_EVENT_TYPE = "PROCESS_INSTANCE_START";
    private static final String PROCESS_DEFINITION_ID_KEY = "processDefinitionId";
    private static final String VARIABLES_KEY = "variables";
    // User task completed event
    private static final String USER_TASK_COMPLETED_EVENT_TYPE = "USER_TASK_COMPLETED";

    private static final String SIMPLEST_PROCESS = "theSimplestProcess";
    private static final String BUSINESS_KEY = "testBusinessKey";
    private static final String TEST_VALUE = "TestValue";
    private static final String TEST_VARIABLE = "testVariable";

    protected InMemoryRecordFlowableEventListener listener = new InMemoryRecordFlowableEventListener(getTransformers());

    private static final String THE_SIMPLEST_PROCESS = "org/flowable/crystalball/simulator/impl/playback/PlaybackProcessStartTest.testDemo.bpmn20.xml";

    @Test
    public void testProcessInstanceStartEvents() throws Exception {
        recordEvents();

        final SimpleSimulationRun.Builder builder = new SimpleSimulationRun.Builder();
        // init simulation run
        Clock clock = new ThreadLocalClock(new DefaultClockFactory());
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
        config.setClock(clock);
        SimulationProcessEngineFactory simulationProcessEngineFactory = new SimulationProcessEngineFactory(config);
        final ProcessEngineImpl simProcessEngine = simulationProcessEngineFactory.getObject();

        builder.processEngine(simProcessEngine)
                .eventCalendar((new SimpleEventCalendarFactory(clock, new SimulationEventComparator(), listener.getSimulationEvents())).getObject())
                .eventHandlers(getHandlers());
        SimpleSimulationRun simRun = builder.build();

        simRun.execute(new NoExecutionVariableScope());

        checkStatus(simProcessEngine);

        simProcessEngine.getProcessEngineConfiguration().setDatabaseSchemaUpdate("create-drop");
        simProcessEngine.close();
        ProcessEngines.destroy();
    }

    private void recordEvents() {
        Clock clock = new DefaultClockImpl();
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault();
        config.setClock(clock);
        ProcessEngine processEngine = (new RecordableProcessEngineFactory(config, listener)).getObject();

        processEngine.getProcessEngineConfiguration().setClock(clock);
        processEngine.getRepositoryService().createDeployment().addClasspathResource(THE_SIMPLEST_PROCESS).deploy();

        Map<String, Object> variables = new HashMap<>();
        variables.put(TEST_VARIABLE, TEST_VALUE);
        processEngine.getRuntimeService().startProcessInstanceByKey(SIMPLEST_PROCESS, BUSINESS_KEY, variables);
        checkStatus(processEngine);
        EventRecorderTestUtils.closeProcessEngine(processEngine, listener);
        ProcessEngines.destroy();
    }

    private void checkStatus(ProcessEngine processEngine) {
        HistoryService historyService = processEngine.getHistoryService();
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().finished().includeProcessVariables()
                .singleResult();
        assertThat(historicProcessInstance).isNotNull();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        final ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(historicProcessInstance.getProcessDefinitionId()).singleResult();
        assertThat(processDefinition.getKey()).isEqualTo(SIMPLEST_PROCESS);

        assertThat(historicProcessInstance.getProcessVariables())
                .hasSize(1)
                .containsEntry(TEST_VARIABLE, TEST_VALUE);
        assertThat(historicProcessInstance.getBusinessKey()).isEqualTo(BUSINESS_KEY);
    }

    private List<Function<FlowableEvent, SimulationEvent>> getTransformers() {
        List<Function<FlowableEvent, SimulationEvent>> transformers = new ArrayList<>();
        transformers.add(new DeploymentCreateTransformer(DEPLOYMENT_CREATED_EVENT_TYPE, DEPLOYMENT_RESOURCES_KEY));
        transformers.add(new ProcessInstanceCreateTransformer(PROCESS_INSTANCE_START_EVENT_TYPE, PROCESS_DEFINITION_ID_KEY, BUSINESS_KEY, VARIABLES_KEY));
        transformers.add(new UserTaskCompleteTransformer(USER_TASK_COMPLETED_EVENT_TYPE));
        return transformers;
    }

    public static Map<String, SimulationEventHandler> getHandlers() {
        Map<String, SimulationEventHandler> handlers = new HashMap<>();
        handlers.put(DEPLOYMENT_CREATED_EVENT_TYPE, new DeployResourcesEventHandler(DEPLOYMENT_RESOURCES_KEY));
        handlers.put(PROCESS_INSTANCE_START_EVENT_TYPE, new StartProcessByIdEventHandler(PROCESS_DEFINITION_ID_KEY, BUSINESS_KEY, VARIABLES_KEY));
        handlers.put(USER_TASK_COMPLETED_EVENT_TYPE, new PlaybackUserTaskCompleteEventHandler());
        return handlers;
    }
}
