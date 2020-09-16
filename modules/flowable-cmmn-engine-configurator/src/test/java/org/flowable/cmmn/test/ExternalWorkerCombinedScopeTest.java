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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.junit.After;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerCombinedScopeTest extends AbstractProcessEngineIntegrationTest {

    protected Deployment deployment;

    @After
    public void tearDown() throws Exception {
        if (deployment != null) {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/ExternalWorkerCombinedScopeTest.simpleCase.cmmn")
    public void testSimpleCombined() {
        processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/ExternalWorkerCombinedScopeTest.simpleProcess.bpmn20.xml")
                .deploy();

        CaseInstance simpleCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleCase")
                .start();

        ProcessInstance simpleProcess = processEngineRuntimeService.createProcessInstanceQuery().singleResult();

        List<AcquiredExternalWorkerJob> cmmnAcquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("customer", Duration.ofMinutes(10))
                .onlyCmmn()
                .acquireAndLock(1, "cmmnWorker");

        AcquiredExternalWorkerJob cmmnAcquiredJob = cmmnAcquiredJobs.get(0);

        assertThat(cmmnAcquiredJob.getScopeId()).isEqualTo(simpleCase.getId());
        assertThat(cmmnAcquiredJob.getScopeType()).isEqualTo(ScopeTypes.CMMN);

        assertThatThrownBy(() -> processEngineManagementService.createExternalWorkerCompletionBuilder(cmmnAcquiredJob.getId(), "cmmnWorker").complete())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("External worker job with id " + cmmnAcquiredJob.getId()
                        + " is not bpmn scoped. This command can only handle bpmn scoped external worker jobs");

        assertThatThrownBy(
                () -> processEngineManagementService.createExternalWorkerCompletionBuilder(cmmnAcquiredJob.getId(), "cmmnWorker").bpmnError("errorCode"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("External worker job with id " + cmmnAcquiredJob.getId()
                        + " is not bpmn scoped. This command can only handle bpmn scoped external worker jobs");

        cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(cmmnAcquiredJob.getId(), "cmmnWorker").complete();

        List<AcquiredExternalWorkerJob> bpmnAcquiredJobs = cmmnManagementService.createExternalWorkerJobAcquireBuilder()
                .topic("customer", Duration.ofMinutes(10))
                .onlyBpmn()
                .acquireAndLock(1, "bpmnWorker");

        AcquiredExternalWorkerJob bpmnAcquiredJob = bpmnAcquiredJobs.get(0);

        assertThat(bpmnAcquiredJob.getProcessInstanceId()).isEqualTo(simpleProcess.getId());
        assertThat(bpmnAcquiredJob.getScopeId()).isNull();
        assertThat(bpmnAcquiredJob.getScopeType()).isNull();

        assertThatThrownBy(() -> cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(bpmnAcquiredJob.getId(), "bpmnWorker").complete())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("External worker job with id " + bpmnAcquiredJob.getId()
                        + " is not cmmn scoped. This command can only handle cmmn scoped external worker jobs");

        assertThatThrownBy(() -> cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(bpmnAcquiredJob.getId(), "bpmnWorker").terminate())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("External worker job with id " + bpmnAcquiredJob.getId()
                        + " is not cmmn scoped. This command can only handle cmmn scoped external worker jobs");

        processEngineManagementService.createExternalWorkerCompletionBuilder(bpmnAcquiredJob.getId(), "bpmnWorker").complete();

        assertThat(cmmnManagementService.createExternalWorkerJobAcquireBuilder().topic("customer", Duration.ofMinutes(10)).acquireAndLock(1, "worker"))
                .isEmpty();
    }

}
