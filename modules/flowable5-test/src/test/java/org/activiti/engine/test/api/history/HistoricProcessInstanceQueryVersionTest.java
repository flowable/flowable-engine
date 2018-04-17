package org.activiti.engine.test.api.history;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;

public class HistoricProcessInstanceQueryVersionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String DEPLOYMENT_FILE_PATH = "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml";

    private org.flowable.engine.repository.Deployment oldDeployment;
    private org.flowable.engine.repository.Deployment newDeployment;

    protected void setUp() throws Exception {
        super.setUp();
        oldDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        Map<String, Object> startMap = new HashMap<>();
        startMap.put("test", 123);
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, startMap);

        newDeployment = repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        startMap.clear();
        startMap.put("anothertest", 456);
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, startMap);
    }

    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(oldDeployment.getId(), true);
        repositoryService.deleteDeployment(newDeployment.getId(), true);
    }

    public void testHistoricProcessInstanceQueryByProcessDefinitionVersion() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).list().get(0).getProcessDefinitionVersion().intValue());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list().get(0).getProcessDefinitionVersion().intValue());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).list().size());

        // Variables Case
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("test", 123).processDefinitionVersion(1).singleResult();
            assertEquals(1, processInstance.getProcessDefinitionVersion().intValue());
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertEquals(123, variableMap.get("test"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(1).singleResult();
            assertNull(processInstance);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(2).singleResult();
            assertEquals(2, processInstance.getProcessDefinitionVersion().intValue());
            variableMap = processInstance.getProcessVariables();
            assertEquals(456, variableMap.get("anothertest"));
        }
    }

    public void testHistoricProcessInstanceQueryByProcessDefinitionVersionAndKey() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list().size());
    }

    public void testHistoricProcessInstanceOrQueryByProcessDefinitionVersion() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().list().size());

        // Variables Case
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("test", "invalid").processDefinitionVersion(1).endOr().singleResult();
            assertEquals(1, processInstance.getProcessDefinitionVersion().intValue());
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertEquals(123, variableMap.get("test"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionVersion(2).endOr().singleResult();
            assertEquals(2, processInstance.getProcessDefinitionVersion().intValue());
            variableMap = processInstance.getProcessVariables();
            assertEquals(456, variableMap.get("anothertest"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", "invalid").processDefinitionVersion(3).singleResult();
            assertNull(processInstance);
        }
    }
}
