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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author martin.grofcikplanItem
 */
public class CaseInstanceLifecycleListenerTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    protected TestReceiveAllLifecycleListener testReceiveAllLifecycleListener = new TestReceiveAllLifecycleListener();

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        Map<Object, Object> beans = new HashMap<>();
        cmmnEngineConfiguration.setBeans(beans);

        beans.put("delegateListener", new TestDelegateTaskListener());
        beans.put("receiveAll", testReceiveAllLifecycleListener);
    }

    @Test
    @CmmnDeployment
    public void testActiveListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseLifecycleListeners").start();

        assertVariable(caseInstance, "classDelegateVariable", "Hello World");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVar", "planItemIsActive");
    }

    @Test
    @CmmnDeployment
    public void testCompletedListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseLifecycleListeners").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertHistoricVariable(caseInstance.getId(), "classDelegateVariable", "Hello World");
        assertHistoricVariable(caseInstance.getId(), "variableFromDelegateExpression", "Hello World from delegate expression");
        assertHistoricVariable(caseInstance.getId(), "expressionVar", "planItemIsCompleted");
    }

    @Test
    @CmmnDeployment
    public void testTerminatedListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseLifecycleListeners").start();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertHistoricVariable(caseInstance.getId(), "classDelegateVariable", "Hello World");
        assertHistoricVariable(caseInstance.getId(), "variableFromDelegateExpression", "Hello World from delegate expression");
        assertHistoricVariable(caseInstance.getId(), "expressionVar", "planItemIsTerminated");
    }

    private void assertVariable(CaseInstance caseInstance, String varName, String value) {
        String variable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    private void assertHistoricVariable(String caseInstanceId, String varName, String value) {
        String variable = (String) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).variableName(varName).singleResult().getValue();
        assertThat(variable).isEqualTo(value);
    }

    static class TestDelegateTaskListener implements CaseInstanceLifecycleListener {

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
            ((CaseInstanceEntity)caseInstance).setVariable("variableFromDelegateExpression", "Hello World from delegate expression");
        }

    }

}
