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
package org.flowable.engine.test.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.SpannerRetryInterceptor;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Integration and configuration verification tests for Cloud Spanner with Flowable Process Engine.
 */
public class SpannerEngineIntegrationTest {

    private static final String SPANNER_CFG_RESOURCE = "flowable.spanner.cfg.xml";
    private static final String SPANNER_DRIVER_CLASS = "com.google.cloud.spanner.jdbc.JdbcDriver";

    @Test
    public void testFlowableSpannerConfigurationParsing() {
        assumeTrue(isSpannerDriverPresent(), "Spanner JDBC driver not present on classpath (activate -Pspanner profile)");

        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource(SPANNER_CFG_RESOURCE);

        try {
            assertThat(configuration).isNotNull();
            assertThat(configuration.getDatabaseType()).isEqualTo(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
            assertThat(configuration.getDatabaseSchemaUpdate()).isEqualTo("drop-create");
            assertThat(configuration.getJdbcDriver()).isEqualTo(SPANNER_DRIVER_CLASS);
            assertThat(configuration.getJdbcUrl()).contains("jdbc:cloudspanner:");

            assertThat(configuration.getIdGenerator())
                    .isNotNull()
                    .isInstanceOf(StrongUuidGenerator.class);

            Collection<? extends CommandInterceptor> defaultInterceptors = configuration.getDefaultCommandInterceptors();
            assertThat(defaultInterceptors)
                    .isNotNull()
                    .anyMatch(interceptor -> interceptor instanceof SpannerRetryInterceptor);
        } finally {
            if (configuration != null && configuration.getDataSource() instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) configuration.getDataSource()).close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    @Test
    public void testSpannerEngineAgainstEmulator() {
        assumeTrue(isSpannerDriverPresent(), "Spanner JDBC driver not present on classpath");
        assumeTrue(isSpannerEmulatorReachable(), "Cloud Spanner emulator not reachable on localhost:9010");

        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource(SPANNER_CFG_RESOURCE);

        ProcessEngine processEngine = null;
        try {
            processEngine = configuration.buildProcessEngine();
            assertThat(processEngine).isNotNull();

            RepositoryService repositoryService = processEngine.getRepositoryService();
            RuntimeService runtimeService = processEngine.getRuntimeService();
            TaskService taskService = processEngine.getTaskService();
            HistoryService historyService = processEngine.getHistoryService();

            // 1. Deploy sample BPMN process
            BpmnModel bpmnModel = createSampleBpmnModel();
            Deployment deployment = repositoryService.createDeployment()
                    .name("Spanner Integration Deployment")
                    .addBpmnModel("spannerSampleProcess.bpmn20.xml", bpmnModel)
                    .deploy();

            assertThat(deployment).isNotNull();
            assertThat(deployment.getId()).isNotNull();

            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            assertThat(processDefinition).isNotNull();
            assertThat(processDefinition.getKey()).isEqualTo("spannerSampleProcess");

            // 2. Start process instance
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("spannerSampleProcess");
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getId()).isNotNull();

            // 3. Query and complete user task
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("Review Task");
            assertThat(task.getAssignee()).isEqualTo("spannerUser");

            taskService.complete(task.getId());

            // 4. Verify process instance execution finished
            long activeInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .count();
            assertThat(activeInstances).isZero();

            // 5. Verify history persistence
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getEndTime()).isNotNull();

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(historicTaskInstance).isNotNull();
            assertThat(historicTaskInstance.getEndTime()).isNotNull();

        } finally {
            if (processEngine != null) {
                processEngine.close();
            }
        }
    }

    protected BpmnModel createSampleBpmnModel() {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("spannerSampleProcess");
        process.setName("Spanner Sample Process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        startEvent.setName("Start");
        process.addFlowElement(startEvent);

        UserTask userTask = new UserTask();
        userTask.setId("reviewTask");
        userTask.setName("Review Task");
        userTask.setAssignee("spannerUser");
        process.addFlowElement(userTask);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");
        endEvent.setName("End");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "reviewTask"));
        process.addFlowElement(new SequenceFlow("reviewTask", "end"));

        model.addProcess(process);
        return model;
    }

    private static boolean isSpannerDriverPresent() {
        try {
            Class.forName(SPANNER_DRIVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isSpannerEmulatorReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 9010), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
