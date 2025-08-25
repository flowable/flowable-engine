package org.flowable.engine.test.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CallActivityInOutMappingTest extends PluggableFlowableTestCase {

    /*
        The tests below were written to validate the fix for a bug where a call activity (or anything with an in mapping),
        would map in a variable, that gets changed in the first step of the called instance.

        In the original bug, it was an ArrayNode, with each element being passed into the instance.
        Due to being passed by reference, any change in the called instance would overwrite also the root variable.
        However, this was only the case when the first step(s) were synchronous, and the same memory reference is used.
        In the case the first step is async, then the change is not propagated to the root variable, because there's
        a second transaction that gets a new memory reference when the variable is fetched from the database.

        This is a problem for any variable type that implements the MutableVariableType interface (e.g. JSON),
        because the actual value is determined at the end of the command context lifecycle.
        Other variable types, which can be mutable, like e.g. a Date variable, don't have this problem, because
        the value is set to the VariableInstanceEntityImpl at the moment the variable is set (e.g. when doing execution.setVariable).
     */

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithArrayNodeAndAsyncFirstStep.bpmn20.xml" })
    public void testInMappingArrayNodeWithAsyncFirstStep() {

        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithArrayNode();

        // As the first step is an async step, there should be one job.
        // The variable myElementVariable is mapped as 'myInMappedVariable' into the called process instance
        // The variable at this point should not have changed at this point.
        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            JsonNode inMappedJsonNodeVariable = (JsonNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedJsonNodeVariable.path("field").asText()).startsWith("value-");
        }

        // Executing the async job should now change the variable in the child process instances
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(10);
        for (Job job : jobs) {
            managementService.executeJob(job.getId());
        }

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            JsonNode inMappedJsonNodeVariable = (JsonNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedJsonNodeVariable.path("field").asText()).isEqualTo("CHANGED");
        }

        // The variable should not have changed on the root process instance level
        ArrayNode rootArrayNode = (ArrayNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootArrayNode).hasSize(10);
        for (JsonNode rootArrayNodeElement : rootArrayNode) {
            assertThat(rootArrayNodeElement.path("field").asText()).startsWith("value-");
        }

    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithArrayNodeAndSyncFirstStep.bpmn20.xml" })
    public void testInMappingArrayNodeWithSyncFirstStep() {

        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithArrayNode();

        // There is no async step here, so the change to the variable happens immediately in the called child process instance
        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            JsonNode inMappedJsonNodeVariable = (JsonNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedJsonNodeVariable.path("field").asText()).isEqualTo("CHANGED");
        }

        // The variable should not have changed on the root process instance level
        ArrayNode rootArrayNode = (ArrayNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootArrayNode).hasSize(10);
        for (JsonNode rootArrayNodeElement : rootArrayNode) {
            assertThat(rootArrayNodeElement.path("field").asText()).startsWith("value-");
        }

    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingNoMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithJsonNodeAndAsyncFirstStep.bpmn20.xml" })
    public void testInMappingJsonNodeWithAsyncFirstStep() {
        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithJsonNode();

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            ObjectNode inMappedVariable = (ObjectNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedVariable.path("field").asText()).isEqualTo("Hello");
        }

        // Executing the async job should now change the variable in the child process instances
        Job job = managementService.createJobQuery().singleResult();
        managementService.executeJob(job.getId());

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            ObjectNode inMappedVariable = (ObjectNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedVariable.path("field").asText()).isEqualTo("TEST");
        }

        // The variable should not have changed on the root process instance level
        ObjectNode rootDateVariable = (ObjectNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootDateVariable.path("field").asText()).isEqualTo("Hello");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingNoMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithJsonNodeAndSyncFirstStep.bpmn20.xml" })
    public void testInMappingJsonNodeWithSyncFirstStep() {
        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithJsonNode();

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            ObjectNode inMappedVariable = (ObjectNode) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedVariable.path("field").asText()).isEqualTo("TEST");
        }

        // The variable should not have changed on the root process instance level
        ObjectNode rootVariable = (ObjectNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootVariable.path("field").asText()).isEqualTo("Hello");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingNoMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithJsonNodeAndSyncFirstStep.bpmn20.xml" })
    public void testOutMappingArrayNodeWithSyncFirstStep() {

        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithJsonNode();

        Task task = taskService.createTaskQuery().taskName("After change json variable").singleResult();
        taskService.complete(task.getId());

        // The variable should not have changed on the root process instance level
        ObjectNode originalRootVariable = (ObjectNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(originalRootVariable.path("field").asText()).isEqualTo("Hello");

        ObjectNode outMappedVariable = (ObjectNode) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "outMappedVariable");
        assertThat(outMappedVariable.path("field").asText()).isEqualTo("TEST");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingNoMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithDateAndAsyncFirstStep.bpmn20.xml" })
    public void testInMappingDateWithAsyncFirstStep() {

        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithDate();

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            Date inMappedDate = (Date) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedDate.getTime()).isNotEqualTo(1); // not equal, because Date is not a MutableVariableType
        }

        // Executing the async job should now change the variable in the child process instances
        Job job = managementService.createJobQuery().singleResult();
        managementService.executeJob(job.getId());

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            Date inMappedDate = (Date) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedDate.getTime()).isNotEqualTo(1); // not equal, because Date is not a MutableVariableType
        }

        // The variable should not have changed on the root process instance level
        Date rootDateVariable = (Date) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootDateVariable.getTime()).isNotEqualTo(1);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingNoMultiInstance.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityInOutMappingTest.mappingWithDateAndSyncFirstStep.bpmn20.xml" })
    public void testInMappingDateWithSyncFirstStep() {

        ProcessInstanceAndChildProcessInstances processInstanceInfo = startTestProcessInstanceWithDate();

        for (ProcessInstance calledProcessInstance : processInstanceInfo.calledProcessInstances()) {
            Date inMappedDate = (Date) runtimeService.getVariable(calledProcessInstance.getId(), "myInMappedVariable");
            assertThat(inMappedDate.getTime()).isNotEqualTo(1); // not equal, because Date is not a MutableVariableType
        }

        // The variable should not have changed on the root process instance level
        Date rootDateVariable = (Date) runtimeService.getVariable(processInstanceInfo.processInstance().getId(), "myRootVariable");
        assertThat(rootDateVariable.getTime()).isNotEqualTo(1);
    }
    

    protected ProcessInstanceAndChildProcessInstances startTestProcessInstanceWithArrayNode() {
       return startTestProcessInstanceWithMultiInstance(() -> createArrayNodeVariable());
    }

    protected ProcessInstanceAndChildProcessInstances startTestProcessInstanceWithJsonNode() {
        return startTestProcessInstanceNoMultiInstance(() -> {
            Map<String, Object> variables = new HashMap<>();
            ObjectNode objectNode = processEngineConfiguration.getObjectMapper().createObjectNode();
            objectNode.put("field", "Hello");
            ObjectNode nestedObjectNode = objectNode.putObject("nested");
            nestedObjectNode.put("field", "World");
            variables.put("myRootVariable", objectNode);
            return variables;
        });
    }

    protected ProcessInstanceAndChildProcessInstances startTestProcessInstanceWithDate() {
        return startTestProcessInstanceNoMultiInstance(() -> {
            Map<String, Object> variables = new HashMap<>();
            variables.put("myRootVariable", new Date());
            return variables;
        });
    }

    protected ProcessInstanceAndChildProcessInstances startTestProcessInstanceWithMultiInstance(Supplier<Map<String, Object>> variablesSupplier) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mappingProcess", variablesSupplier.get());

        List<ProcessInstance> calledProcessInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(calledProcessInstances).hasSize(10);

        // Only the root variable should be available as a non-local variable
        assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(1);

        // Each execution for each called instance should have a local variable named 'myElementVariable'
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        for (Execution execution : executions) {
            if (!((ExecutionEntityImpl) execution).isMultiInstanceRoot()) {
                assertThat(runtimeService.getVariablesLocal(execution.getId())).containsOnlyKeys("myElementVariable", "loopCounter");
            }
        }

        return new ProcessInstanceAndChildProcessInstances(processInstance, calledProcessInstances);
    }

    protected ProcessInstanceAndChildProcessInstances startTestProcessInstanceNoMultiInstance(Supplier<Map<String, Object>> variablesSupplier) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mappingProcess", variablesSupplier.get());

        // Only the root variable should be available as a non-local variable
        assertThat(runtimeService.getVariables(processInstance.getId())).hasSize(1);

        List<ProcessInstance> calledProcessInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(calledProcessInstances).hasSize(1);

        return new ProcessInstanceAndChildProcessInstances(processInstance, calledProcessInstances);
    }

    private Map<String, Object> createArrayNodeVariable() {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < 10; i++) {
            ObjectNode arrayElement = objectMapper.createObjectNode();
            arrayElement.put("field", "value-" + i);
            arrayNode.add(arrayElement);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("myRootVariable", arrayNode);
        return variables;
    }

    private record ProcessInstanceAndChildProcessInstances(ProcessInstance processInstance, List<ProcessInstance> calledProcessInstances) {}

}
