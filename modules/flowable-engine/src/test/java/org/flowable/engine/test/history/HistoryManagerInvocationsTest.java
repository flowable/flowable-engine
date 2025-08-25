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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Joram Barrez
 */
public class HistoryManagerInvocationsTest extends CustomConfigurationFlowableTestCase {

    private HistoryManager mockHistoryManager;

    public HistoryManagerInvocationsTest() {
        super(HistoryManagerInvocationsTest.class.getName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.mockHistoryManager = Mockito.mock(HistoryManager.class);
        processEngineConfiguration.setHistoryManager(mockHistoryManager);
    }

    @Test
    public void testSingleTaskCreateAndComplete() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionId(deployOneTaskTestProcess()).start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        verify(mockHistoryManager, times(1)).recordTaskCreated(any(), any());
        verify(mockHistoryManager, times(1)).recordTaskEnd(any(), any(), any(), any(), any());

        verify(mockHistoryManager, times(1)).recordProcessInstanceStart(any());
        verify(mockHistoryManager, times(1)).recordProcessInstanceEnd(any(), any(), any(), any(), any());

    }

}
