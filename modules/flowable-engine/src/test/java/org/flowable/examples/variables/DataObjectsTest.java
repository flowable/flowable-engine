package org.flowable.examples.variables;

import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class DataObjectsTest extends PluggableFlowableTestCase {
    @Test
    @Deployment
    public void testRetrieveDataObjectsFromNestedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("DataObjectsTest");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("usertask2", task.getTaskDefinitionKey());

        Execution subProcess1 = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess1").singleResult();
        Execution subProcess2 = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess2").singleResult();

        Map<String, DataObject> dataObjects = runtimeService.getDataObjects(processInstance.getId());
        assert 2 == dataObjects.size();
        assertNotNull(dataObjects.get("VariableA"));
        assertNotNull(dataObjects.get("VariableB"));

        assertNotNull(runtimeService.getDataObject(processInstance.getId(), "VariableA"));
        assertNotNull(runtimeService.getDataObject(processInstance.getId(), "VariableB"));

        dataObjects = runtimeService.getDataObjects(subProcess1.getId());
        assert 3 == dataObjects.size();

        assertNotNull(dataObjects.get("VariableA"));
        assertNotNull(dataObjects.get("VariableB"));
        assertNotNull(dataObjects.get("VariableC"));

        assertNotNull(runtimeService.getDataObject(subProcess1.getId(), "VariableA"));
        assertNotNull(runtimeService.getDataObject(subProcess1.getId(), "VariableB"));
        assertNotNull(runtimeService.getDataObject(subProcess1.getId(), "VariableC"));

        dataObjects = runtimeService.getDataObjects(subProcess2.getId());
        assert 4 == dataObjects.size();

        assertNotNull(dataObjects.get("VariableA"));
        assertNotNull(dataObjects.get("VariableB"));
        assertNotNull(dataObjects.get("VariableC"));
        assertNotNull(dataObjects.get("VariableD"));

        assertNotNull(runtimeService.getDataObject(subProcess2.getId(), "VariableA"));
        assertNotNull(runtimeService.getDataObject(subProcess2.getId(), "VariableB"));
        assertNotNull(runtimeService.getDataObject(subProcess2.getId(), "VariableC"));
        assertNotNull(runtimeService.getDataObject(subProcess2.getId(), "VariableD"));

        dataObjects = taskService.getDataObjects(task.getId());
        assert 4 == dataObjects.size();

        assertNotNull(dataObjects.get("VariableA"));
        assertNotNull(dataObjects.get("VariableB"));
        assertNotNull(dataObjects.get("VariableC"));
        assertNotNull(dataObjects.get("VariableD"));

        assertNotNull(taskService.getDataObject(task.getId(), "VariableA"));
        assertNotNull(taskService.getDataObject(task.getId(), "VariableB"));
        assertNotNull(taskService.getDataObject(task.getId(), "VariableC"));
        assertNotNull(taskService.getDataObject(task.getId(), "VariableD"));
    }
}
