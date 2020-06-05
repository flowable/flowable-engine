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
import static org.assertj.core.api.Assertions.entry;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class ServiceTaskDelegateExpressionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testDelegateExpression() {
        String processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(
                        entry("executed", true)
                );

        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();
    }

    @Test
    @Deployment
    public void testDelegateExpressionWithSkipExpression() {
        // Global property enabled, but local says false -> do not skip
        String processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true)
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", false)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(
                        entry("executed", true)
                );

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();

        // Global property enabled, but local says true -> do skip
        processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true)
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", true)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId)).isEmpty();

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();

        // Global property not enabled, but local says true -> do not skip
        processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", true)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(
                        entry("executed", true)
                );

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();
    }
}
