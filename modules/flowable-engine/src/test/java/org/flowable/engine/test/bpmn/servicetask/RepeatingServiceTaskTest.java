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

package org.flowable.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class RepeatingServiceTaskTest extends PluggableFlowableTestCase {

    @Deployment
    @Test
    public void testMultipleInvocationsInSameTransaction() {
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("repeating",
                        CollectionUtil.singletonMap("count", 0));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .variableName("count")
                    .singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat((Integer) historicVariableInstance.getValue()).isEqualTo(1001);
        }

        assertThat(processInstance.isEnded()).isTrue();
    }
}
