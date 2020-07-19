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
package org.flowable.engine.test.bpmn.callactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link org.flowable.engine.impl.bpmn.behavior.CallActivityBehavior} with calledElementType id
 */
public class CallActivityWithElementType extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources =
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml")
    public void testCallSimpleSubProcessByKey() throws IOException {
        assertThatSubProcessIsCalled(
            createCallProcess("key", "simpleSubProcess"),
            Collections.emptyMap()
        );
    }

    @Test
    @Deployment(resources =
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml")
    public void testCallSimpleSubProcessById() throws IOException {
        String subProcessDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simpleSubProcess").singleResult().getId();

        assertThatSubProcessIsCalled(
            createCallProcess("id", subProcessDefinitionId),
            Collections.emptyMap()
        );
    }

    @Test
    @Deployment(resources =
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml")
    public void testCallSimpleSubProcessByIdExpression() throws IOException {
        String subProcessDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey("simpleSubProcess").singleResult().getId();

        assertThatSubProcessIsCalled(
            createCallProcess("id", "${subProcessDefinitionId}"),
            Collections.singletonMap("subProcessDefinitionId", subProcessDefinitionId)
        );
    }

    @Test
    @Deployment(resources =
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml")
    public void testCallSimpleSubProcessByKeyExpression() throws IOException {
        repositoryService.createProcessDefinitionQuery().processDefinitionKey("simpleSubProcess").singleResult().getId();

        assertThatSubProcessIsCalled(
            createCallProcess("key", "${subProcessDefinitionKey}"),
            Collections.singletonMap("subProcessDefinitionKey", "simpleSubProcess")
        );
    }

    @Test
    @Deployment(resources =
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml")
    public void testCallSimpleSubProcessWithUnrecognizedElementType() throws IOException {
        assertThatThrownBy(() -> assertThatSubProcessIsCalled(
                createCallProcess("unrecognizedElementType", "simpleSubProcess"),
                        Collections.singletonMap("subProcessDefinitionKey", "simpleSubProcess")))
                .as("Flowable exception expected")
                .isInstanceOf(FlowableException.class)
                .hasMessage("Unrecognized calledElementType [unrecognizedElementType]");
    }

    protected void assertThatSubProcessIsCalled(String deploymentId, Map<String, Object> variables) {
        try {
            runtimeService.startProcessInstanceByKey("callSimpleSubProcess", variables);

            // one task in the subprocess should be active after starting the
            // process instance
            TaskQuery taskQuery = taskService.createTaskQuery();
            Task taskBeforeSubProcess = taskQuery.singleResult();
            assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

            // Completing the task continues the process which leads to calling the
            // subprocess
            taskService.complete(taskBeforeSubProcess.getId());
            Task taskInSubProcess = taskQuery.singleResult();
            assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
        } finally {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    protected String createCallProcess(String calledElementType, String calledElement) throws IOException {
        return repositoryService.createDeployment().
            addString("CallActivity.testCallSimpleSubProcessWithParametrizedCalledElement.bpmn20.xml",
                IOUtils.resourceToString(
                    "/org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithParametrizedCalledElement.bpmn20.xml",
                    Charset.defaultCharset())
                    .replace("{calledElementType}", calledElementType)
                    .replace("{calledElement}", calledElement)
            )
            .deploy()
            .getId();
    }

}
