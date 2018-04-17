package org.activiti.engine.test.api.history;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;

public class NonCascadeDeleteTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

    private String deploymentId;

    private String processInstanceId;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHistoricProcessInstanceQuery() {
        deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .deploy().getId();

        processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertEquals(PROCESS_DEFINITION_KEY, processInstance.getProcessDefinitionKey());

            // Delete deployment and historic process instance remains.
            repositoryService.deleteDeployment(deploymentId, false);

            HistoricProcessInstance processInstanceAfterDelete = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertNull(processInstanceAfterDelete.getProcessDefinitionKey());
            assertNull(processInstanceAfterDelete.getProcessDefinitionName());
            assertNull(processInstanceAfterDelete.getProcessDefinitionVersion());

            // clean
            historyService.deleteHistoricProcessInstance(processInstanceId);
        }
    }
}
