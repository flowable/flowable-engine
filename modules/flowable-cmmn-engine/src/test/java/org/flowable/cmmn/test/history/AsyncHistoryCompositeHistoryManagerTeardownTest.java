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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.CompositeCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.DefaultCmmnHistoryManager;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * With async history enabled, the test teardown must still go through the configured (composite) history manager,
 * so that additional history managers see the deletion of the historic case instances.
 *
 * @author Filip Hrisafov
 */
public class AsyncHistoryCompositeHistoryManagerTeardownTest extends CustomCmmnConfigurationFlowableTestCase {

    protected static CmmnHistoryManager additionalHistoryManager;
    protected static String annotationDeploymentCaseInstanceId;

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        additionalHistoryManager = Mockito.mock(CmmnHistoryManager.class);
        cmmnEngineConfiguration.setAsyncHistoryEnabled(true);
        cmmnEngineConfiguration.setCmmnHistoryManager(new CompositeCmmnHistoryManager(
                List.of(new DefaultCmmnHistoryManager(cmmnEngineConfiguration), additionalHistoryManager)));
    }

    @AfterAll
    static void verifyAnnotationDeploymentTeardownInvokedAdditionalHistoryManager() {
        assertThat(annotationDeploymentCaseInstanceId).isNotNull();
        verify(additionalHistoryManager).recordHistoricCaseInstanceDeleted(annotationDeploymentCaseInstanceId, "");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void annotationDeploymentTeardownShouldInvokeAdditionalHistoryManager() {
        assertThat(cmmnEngineConfiguration.isAsyncHistoryEnabled()).isTrue();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        annotationDeploymentCaseInstanceId = caseInstance.getId();
    }

    @Test
    public void testHelperDeleteDeploymentShouldInvokeAdditionalHistoryManager() {
        assertThat(cmmnEngineConfiguration.isAsyncHistoryEnabled()).isTrue();
        String deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy()
                .getId();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deploymentId);

        verify(additionalHistoryManager).recordHistoricCaseInstanceDeleted(caseInstance.getId(), "");
        assertThat(cmmnManagementService.createHistoryJobQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    public void baseTestCaseDeleteDeploymentShouldInvokeAdditionalHistoryManager() {
        assertThat(cmmnEngineConfiguration.isAsyncHistoryEnabled()).isTrue();
        String deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy()
                .getId();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        deleteDeployment(deploymentId);

        verify(additionalHistoryManager).recordHistoricCaseInstanceDeleted(caseInstance.getId(), "");
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
}
