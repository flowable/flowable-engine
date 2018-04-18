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
package org.flowable.cmmn.test.task;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * author martin.grofcik
 */
public class CmmnTaskServiceIdGeneratorTest extends FlowableCmmnTestCase {

    private static CmmnEngineConfiguration previousCmmnEngineConfiguration;

    @BeforeClass
    public static void initCmmnEngine() {
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream("org/flowable/cmmn/test/task/taskid.generator.flowable.cmmn.cfg.xml")) {
            CmmnEngine cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            previousCmmnEngineConfiguration = CmmnTestRunner.getCmmnEngineConfiguration();
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngine.getCmmnEngineConfiguration());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void restoreCmmnEngine() {
        CmmnTestRunner.setCmmnEngineConfiguration(previousCmmnEngineConfiguration);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testOneHumanTaskCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);
        assertThat(task.getId(), startsWith("TASK-"));

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicTaskInstance);
            assertThat(historicTaskInstance.getId(), startsWith("TASK-"));
        }

        cmmnTaskService.complete(task.getId());
    }

}
