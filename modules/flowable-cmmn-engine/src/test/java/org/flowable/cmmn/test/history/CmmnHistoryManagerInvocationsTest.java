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
package org.flowable.cmmn.test.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryManagerInvocationsTest extends CustomCmmnConfigurationFlowableTestCase {

    private CmmnHistoryManager mockHistoryManager;

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.mockHistoryManager = Mockito.mock(CmmnHistoryManager.class);
        cmmnEngineConfiguration.setCmmnHistoryManager(mockHistoryManager);
    }

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Test
    public void testSingleTaskCreateAndComplete() {
        CaseInstance caseInstance = deployAndStartOneHumanTaskCaseModel();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        verify(mockHistoryManager, times(1)).recordTaskCreated(any());
        verify(mockHistoryManager, times(1)).recordTaskEnd(any(), any(), any());

        verify(mockHistoryManager, times(1)).recordCaseInstanceStart(any());
        verify(mockHistoryManager, times(1)).recordCaseInstanceEnd(any(), any(), any());
    }

}
