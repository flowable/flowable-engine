package org.flowable.engine.test.bpmn.multiinstance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ParallelMultiInstanceAsyncTest extends PluggableFlowableTestCase {

    @AfterEach
    public void cleanup() {
        repositoryService.createDeploymentQuery().list().forEach(deployment -> repositoryService.deleteDeployment(deployment.getId(), true));
    }

    @Test
    public void testAsyncNonExclusiveParallelMultiInstanceSubProcess() {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/multiinstance/parallelMISubProcessTest.bpmn")
                .addClasspathResource("org/flowable/engine/test/bpmn/multiinstance/testScriptSubProcess.bpmn")
                .deploy();

        Map<String, Object> variables = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < 10; i++) {
            ObjectNode varNode = arrayNode.addObject();
            varNode.put("value", i + "");
        }
        variables.put("array", arrayNode);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelSubprocessTest", variables);

        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 60000L, 1000L, new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return taskService.createTaskQuery().count() == 1;
            }
        });

        // User task
        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getTaskDefinitionKey()).isEqualTo("formTask1");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }
}
