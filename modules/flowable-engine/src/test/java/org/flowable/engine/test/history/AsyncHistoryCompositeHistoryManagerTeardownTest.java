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
package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.CompositeHistoryManager;
import org.flowable.engine.impl.history.DefaultHistoryManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * With async history enabled, the test teardown must still go through the configured (composite) history manager,
 * so that additional history managers see the deletion of the historic process instances.
 *
 * @author Filip Hrisafov
 */
public class AsyncHistoryCompositeHistoryManagerTeardownTest extends CustomConfigurationFlowableTestCase {

    protected static HistoryManager additionalHistoryManager;
    protected static String annotationDeploymentProcessDefinitionId;

    public AsyncHistoryCompositeHistoryManagerTeardownTest() {
        super(AsyncHistoryCompositeHistoryManagerTeardownTest.class.getName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        additionalHistoryManager = Mockito.mock(HistoryManager.class);
        processEngineConfiguration.setAsyncHistoryEnabled(true);
        processEngineConfiguration.setHistoryManager(new CompositeHistoryManager(
                List.of(new DefaultHistoryManager(processEngineConfiguration), additionalHistoryManager)));
    }

    @AfterAll
    static void verifyAnnotationDeploymentTeardownInvokedAdditionalHistoryManager() {
        assertThat(annotationDeploymentProcessDefinitionId).isNotNull();
        verify(additionalHistoryManager).recordDeleteHistoricProcessInstancesByProcessDefinitionId(annotationDeploymentProcessDefinitionId);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void annotationDeploymentTeardownShouldInvokeAdditionalHistoryManager() {
        assertThat(processEngineConfiguration.isAsyncHistoryEnabled()).isTrue();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        annotationDeploymentProcessDefinitionId = processInstance.getProcessDefinitionId();
    }

    @Test
    public void baseTestCaseDeleteDeploymentShouldInvokeAdditionalHistoryManager() {
        assertThat(processEngineConfiguration.isAsyncHistoryEnabled()).isTrue();
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploy()
                .getId();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        deleteDeployment(deploymentId);

        verify(additionalHistoryManager).recordDeleteHistoricProcessInstancesByProcessDefinitionId(processInstance.getProcessDefinitionId());
        assertThat(managementService.createHistoryJobQuery().count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
}
