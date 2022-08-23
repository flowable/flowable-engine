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
package org.flowable.examples.bpmn.executionlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.scripting.FlowableScriptEvaluationException;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

public class ScriptTypeExecutionListenerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testExecutionListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("scriptExecutionListenerProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId())
                    .list();
            assertThat(historicVariables)
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .containsExactlyInAnyOrder(
                            tuple("varSetInScript", "yes"),
                            tuple("myVar", "BAR")
                    );
        }
    }

    @Test
    @Deployment
    public void testThrowFlowableIllegalArgumentException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptExecutionListenerProcess"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @Deployment
    public void testThrowNonFlowableException() {
        ProcessDefinition processDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("scriptExecutionListenerProcess")
                .singleResult();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptExecutionListenerProcess"))
                .isInstanceOf(FlowableScriptEvaluationException.class)
                .hasMessage(
                        "JavaScript script evaluation failed: 'java.lang.RuntimeException: Illegal argument in listener in <eval> at line number 2 at column number 28' "
                                + "Trace: scopeType=bpmn, scopeDefinitionKey=scriptExecutionListenerProcess, scopeDefinitionId=" + processDef.getId() + ","
                                + " subScopeDefinitionKey=flow1, tenantId=<empty>, type=executionListener")
                .rootCause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Illegal argument in listener");
    }
}
