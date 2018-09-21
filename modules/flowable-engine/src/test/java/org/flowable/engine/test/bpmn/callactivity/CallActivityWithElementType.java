package org.flowable.engine.test.bpmn.callactivity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
        try {
            assertThatSubProcessIsCalled(
                createCallProcess("unrecognizedElementType", "simpleSubProcess"),
                Collections.singletonMap("subProcessDefinitionKey", "simpleSubProcess")
            );
            fail("Flowable exception expected");
        } catch (FlowableException e) {
            assertThat(e).hasMessage("Unrecognized calledElementType [unrecognizedElementType]");
        }
    }

    protected void assertThatSubProcessIsCalled(String deploymentId, Map<String, Object> variables) {
        try {
            runtimeService.startProcessInstanceByKey("callSimpleSubProcess", variables);

            // one task in the subprocess should be active after starting the
            // process instance
            TaskQuery taskQuery = taskService.createTaskQuery();
            Task taskBeforeSubProcess = taskQuery.singleResult();
            assertEquals("Task before subprocess", taskBeforeSubProcess.getName());

            // Completing the task continues the process which leads to calling the
            // subprocess
            taskService.complete(taskBeforeSubProcess.getId());
            Task taskInSubProcess = taskQuery.singleResult();
            assertEquals("Task in subprocess", taskInSubProcess.getName());
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
