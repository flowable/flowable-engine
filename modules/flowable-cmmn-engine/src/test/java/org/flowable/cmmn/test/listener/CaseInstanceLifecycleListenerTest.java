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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author martin.grofcik
 * @author Joram Barrez
 * @author Filip Hrisafov
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

        cmmnEngineConfiguration.addCaseInstanceLifeCycleListener(new CaseInstanceLifecycleListener() {

            @Override
            public String getSourceState() {
                return null;
            }
            @Override
            public String getTargetState() {
                return CaseInstanceState.ACTIVE;
            }
            @Override
            public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
                ((CaseInstanceEntity)caseInstance).setVariable("globalActiveVariable", "ok");
            }
        });

        cmmnEngineConfiguration.addCaseInstanceLifeCycleListener(new CaseInstanceLifecycleListener() {

            @Override
            public String getSourceState() {
                return null;
            }
            @Override
            public String getTargetState() {
                return CaseInstanceState.COMPLETED;
            }
            @Override
            public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
                ((CaseInstanceEntity)caseInstance).setVariable("globalCompletedVariable", "ok");
            }
        });

        cmmnEngineConfiguration.addCaseInstanceLifeCycleListener(new CaseInstanceLifecycleListener() {

            @Override
            public String getSourceState() {
                return null;
            }
            @Override
            public String getTargetState() {
                return CaseInstanceState.TERMINATED;
            }
            @Override
            public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
                ((CaseInstanceEntity)caseInstance).setVariable("globalTerminatedVariable", "ok");
            }
        });
    }

    @Test
    @CmmnDeployment
    public void testActiveListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseLifecycleListeners").start();

        assertVariable(caseInstance, "classDelegateVariable", "Hello World");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVar", "planItemIsActive");

        assertVariable(caseInstance, "globalActiveVariable", "ok");
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

        assertHistoricVariable(caseInstance.getId(), "globalActiveVariable", "ok");
        assertHistoricVariable(caseInstance.getId(), "globalCompletedVariable", "ok");
        assertHistoricVariable(caseInstance.getId(), "globalTerminatedVariable", null);
    }

    @Test
    @CmmnDeployment
    public void testTerminatedListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseLifecycleListeners").start();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertHistoricVariable(caseInstance.getId(), "classDelegateVariable", "Hello World");
        assertHistoricVariable(caseInstance.getId(), "variableFromDelegateExpression", "Hello World from delegate expression");
        assertHistoricVariable(caseInstance.getId(), "expressionVar", "planItemIsTerminated");

        assertHistoricVariable(caseInstance.getId(), "globalActiveVariable", "ok");
        assertHistoricVariable(caseInstance.getId(), "globalCompletedVariable", null);
        assertHistoricVariable(caseInstance.getId(), "globalTerminatedVariable", "ok");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/CaseInstanceLifecycleListenerDelegateExpressionThrowsException.cmmn")
    public void testListenerWithDelegateExpressionThrowsFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseLifecycleListeners")
                .transientVariable("bean", new CaseInstanceLifecycleListener() {

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
                        throw new FlowableIllegalArgumentException("Message from listener");
                    }
                });
        assertThatThrownBy(builder::start)
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/CaseInstanceLifecycleListenerDelegateExpressionThrowsException.cmmn")
    public void testListenerWithDelegateExpressionThrowsNonFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseLifecycleListeners")
                .transientVariable("bean", new CaseInstanceLifecycleListener() {

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
                        throw new RuntimeException("Message from listener");
                    }
                });
        assertThatThrownBy(builder::start)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    @Test
    @CmmnDeployment
    public void testListenerWithClassThrowsFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseLifecycleListeners");
        assertThatThrownBy(builder::start)
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @CmmnDeployment
    public void testListenerWithClassThrowsNonFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseLifecycleListeners");
        assertThatThrownBy(builder::start)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    private void assertVariable(CaseInstance caseInstance, String varName, String value) {
        String variable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    private void assertHistoricVariable(String caseInstanceId, String varName, String value) {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).variableName(varName).singleResult();
            String variableValue = null;
            if (historicVariableInstance != null) {
                variableValue = (String) historicVariableInstance.getValue();
            }
            assertThat(variableValue).isEqualTo(value);
        }
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

    public static class ThrowingFlowableExceptionCaseInstanceLifecycleListener implements CaseInstanceLifecycleListener {

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
            throw new FlowableIllegalArgumentException("Illegal argument in listener");

        }
    }

    public static class ThrowingNonFlowableExceptionCaseInstanceLifecycleListener implements CaseInstanceLifecycleListener {

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
            throw new RuntimeException("Illegal argument in listener");

        }
    }

}
