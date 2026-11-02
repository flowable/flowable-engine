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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.interceptor.CmmnChildInstanceParametersInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.interceptor.ChildInstanceInParametersContext;
import org.flowable.engine.interceptor.ChildInstanceOutParametersContext;
import org.flowable.engine.interceptor.ChildInstanceParametersInterceptor;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The child instance parameters interceptors of both engines: called after the in parameters of a call activity, case
 * task or process task were evaluated, and after its out parameters were applied.
 */
public class ChildInstanceParametersInterceptorTest extends AbstractProcessEngineIntegrationTest {

    private static final String RESOURCES = "org/flowable/cmmn/test/ChildInstanceParametersInterceptorTest.";

    /** The ids of the tasks the interceptors were called for after the out parameters, in order. */
    private final List<String> afterOutParametersCalls = new ArrayList<>();

    @BeforeEach
    public void installInterceptors() {
        // In: the interceptor sees the evaluated in parameters, and adds a start variable of its own.
        // Out: the interceptor reads that variable back from the completed child, next to the out parameters.
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setChildInstanceParametersInterceptor(new ChildInstanceParametersInterceptor() {

            @Override
            public void afterInParameters(ChildInstanceInParametersContext context) {
                context.getVariables().put("orderIdSeenByInterceptor", context.getVariables().get("orderId"));
                context.getVariables().put("addedByInterceptor", context.getFlowElement().getId());
            }

            @Override
            public void afterOutParameters(ChildInstanceOutParametersContext context) {
                afterOutParametersCalls.add(context.getFlowElement().getId());
                context.getExecution().setVariable("readFromChild", context.getChildInstance().getVariable("addedByInterceptor"));
            }
        });

        cmmnEngineConfiguration.setChildInstanceParametersInterceptor(new CmmnChildInstanceParametersInterceptor() {

            @Override
            public void afterInParameters(org.flowable.cmmn.engine.interceptor.ChildInstanceInParametersContext context) {
                context.getVariables().put("orderIdSeenByInterceptor", context.getVariables().get("orderId"));
                context.getVariables().put("addedByInterceptor", context.getChildTask().getId());
            }

            @Override
            public void afterOutParameters(org.flowable.cmmn.engine.interceptor.ChildInstanceOutParametersContext context) {
                afterOutParametersCalls.add(context.getChildTask().getId());
                context.getPlanItemInstance().setVariable("readFromChild", context.getChildInstance().getVariable("addedByInterceptor"));
            }
        });
    }

    @AfterEach
    public void removeInterceptors() {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setChildInstanceParametersInterceptor(null);
        cmmnEngineConfiguration.setChildInstanceParametersInterceptor(null);
    }

    @Test
    public void testCallActivity() {
        Deployment deployment = deployProcesses("callActivity.bpmn20.xml", "childProcess.bpmn20.xml");
        try {
            ProcessInstance parent = processEngineRuntimeService.startProcessInstanceByKey("interceptorCallActivity", Map.of("orderId", "ORD-1"));

            ProcessInstance child = processEngineRuntimeService.createProcessInstanceQuery().superProcessInstanceId(parent.getId()).singleResult();
            assertThat(processEngineRuntimeService.getVariables(child.getId()))
                    .containsEntry("orderId", "ORD-1")
                    .containsEntry("orderIdSeenByInterceptor", "ORD-1")
                    .containsEntry("addedByInterceptor", "childTask");

            completeChildProcess(child);

            assertThat(processEngineRuntimeService.getVariables(parent.getId()))
                    .containsEntry("reviewer", "kermit")
                    .containsEntry("readFromChild", "childTask");
            assertThat(afterOutParametersCalls).containsExactly("childTask");
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = RESOURCES + "childCase.cmmn")
    public void testBpmnCaseTask() {
        Deployment deployment = deployProcesses("bpmnCaseTask.bpmn20.xml");
        try {
            ProcessInstance parent = processEngineRuntimeService.startProcessInstanceByKey("interceptorBpmnCaseTask", Map.of("orderId", "ORD-1"));

            CaseInstance child = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("interceptorChildCase").singleResult();
            assertThat(cmmnRuntimeService.getVariables(child.getId()))
                    .containsEntry("orderId", "ORD-1")
                    .containsEntry("orderIdSeenByInterceptor", "ORD-1")
                    .containsEntry("addedByInterceptor", "childTask");

            completeChildCase(child.getId());

            assertThat(processEngineRuntimeService.getVariables(parent.getId()))
                    .containsEntry("reviewer", "kermit")
                    .containsEntry("readFromChild", "childTask");
            assertThat(afterOutParametersCalls).containsExactly("childTask");
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = RESOURCES + "immediateChildCase.cmmn")
    public void testBpmnCaseTaskWithChildCaseCompletingWhileStarting() {
        Deployment deployment = deployProcesses("bpmnImmediateCaseTask.bpmn20.xml");
        try {
            ProcessInstance parent = processEngineRuntimeService.startProcessInstanceByKey("interceptorBpmnImmediateCaseTask",
                    Map.of("orderId", "ORD-1"));

            assertThat(processEngineRuntimeService.getVariables(parent.getId())).containsEntry("readFromChild", "childTask");
            assertThat(afterOutParametersCalls).containsExactly("childTask");
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = RESOURCES + "processTask.cmmn")
    public void testProcessTask() {
        Deployment deployment = deployProcesses("childProcess.bpmn20.xml");
        try {
            CaseInstance parent = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("interceptorProcessTaskCase")
                    .variable("orderId", "ORD-1")
                    .start();

            ProcessInstance child = processEngineRuntimeService.createProcessInstanceQuery()
                    .processInstanceId(childReferenceId(parent))
                    .singleResult();
            assertThat(processEngineRuntimeService.getVariables(child.getId()))
                    .containsEntry("orderId", "ORD-1")
                    .containsEntry("orderIdSeenByInterceptor", "ORD-1")
                    .containsEntry("addedByInterceptor", "childTask");

            completeChildProcess(child);

            assertThat(cmmnRuntimeService.getVariables(parent.getId()))
                    .containsEntry("reviewer", "kermit")
                    .containsEntry("readFromChild", "childTask");
            assertThat(afterOutParametersCalls).containsExactly("childTask");
        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = { RESOURCES + "caseTask.cmmn", RESOURCES + "childCase.cmmn" })
    public void testCaseTask() {
        CaseInstance parent = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("interceptorCaseTaskCase")
                .variable("orderId", "ORD-1")
                .start();

        String childCaseId = childReferenceId(parent);
        assertThat(cmmnRuntimeService.getVariables(childCaseId))
                .containsEntry("orderId", "ORD-1")
                .containsEntry("orderIdSeenByInterceptor", "ORD-1")
                .containsEntry("addedByInterceptor", "childTask");

        completeChildCase(childCaseId);

        assertThat(cmmnRuntimeService.getVariables(parent.getId()))
                .containsEntry("reviewer", "kermit")
                .containsEntry("readFromChild", "childTask");
        // The out parameters of a completed child case are handled when it completes, and the case task is triggered
        // afterwards: the interceptor is called once.
        assertThat(afterOutParametersCalls).containsExactly("childTask");
    }

    private Deployment deployProcesses(String... resources) {
        org.flowable.engine.repository.DeploymentBuilder builder = processEngineRepositoryService.createDeployment();
        for (String resource : resources) {
            builder.addClasspathResource(RESOURCES + resource);
        }
        return builder.deploy();
    }

    private String childReferenceId(CaseInstance parent) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(parent.getId())
                .planItemDefinitionId("childTask")
                .singleResult()
                .getReferenceId();
    }

    private void completeChildProcess(ProcessInstance child) {
        Execution review = processEngineRuntimeService.createExecutionQuery().processInstanceId(child.getId()).activityId("review").singleResult();
        processEngineRuntimeService.trigger(review.getId(), Map.of("reviewer", "kermit"));
    }

    private void completeChildCase(String childCaseId) {
        cmmnRuntimeService.setVariable(childCaseId, "reviewer", "kermit");
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(childCaseId)
                .planItemDefinitionId("review")
                .singleResult()
                .getId());
    }
}
