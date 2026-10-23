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
package org.activiti.standalone.history;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.activiti.engine.test.history.SerializableVariable;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;

/**
 * Full history tests for all variable types, including serializable variables, which need to be enabled explicitly.
 *
 * @author Tom Baeyens
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class FullHistoryAllVariableTypesTest extends ResourceFlowableTestCase {

    public FullHistoryAllVariableTypesTest() {
        super("org/activiti/engine/test/serializable-variables.flowable.cfg.xml");
    }

    @Deployment(resources = "org/activiti/standalone/history/FullHistoryTest.testHistoricVariableUpdatesAllTypes.bpmn20.xml")
    public void testHistoricVariableUpdatesAllTypes() throws Exception {
        Clock clock = processEngineConfiguration.getClock();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss SSS");
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("aVariable", "initial value");

        Date startedDate = sdf.parse("01/01/2001 01:23:45 000");

        // In the javaDelegate, the current time is manipulated
        Date updatedDate = sdf.parse("01/01/2001 01:23:46 000");

        clock.setCurrentTime(startedDate);
        processEngineConfiguration.setClock(clock);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HistoricVariableUpdateProcess", variables);

        List<HistoricDetail> details = historyService.createHistoricDetailQuery()
                .variableUpdates()
                .processInstanceId(processInstance.getId())
                .orderByVariableName().asc()
                .orderByTime().asc()
                .list();

        // 8 variable updates should be present, one performed when starting process
        // the other 7 are set in VariableSetter serviceTask
        assertEquals(9, details.size());

        // Since we order by varName, first entry should be aVariable update from startTask
        HistoricVariableUpdate startVarUpdate = (HistoricVariableUpdate) details.get(0);
        assertEquals("aVariable", startVarUpdate.getVariableName());
        assertEquals("initial value", startVarUpdate.getValue());
        assertEquals(0, startVarUpdate.getRevision());
        assertEquals(processInstance.getId(), startVarUpdate.getProcessInstanceId());
        // Date should the one set when starting
        assertEquals(startedDate, startVarUpdate.getTime());

        HistoricVariableUpdate updatedStringVariable = (HistoricVariableUpdate) details.get(1);
        assertEquals("aVariable", updatedStringVariable.getVariableName());
        assertEquals("updated value", updatedStringVariable.getValue());
        assertEquals(processInstance.getId(), updatedStringVariable.getProcessInstanceId());
        // Date should be the updated date
        assertEquals(updatedDate, updatedStringVariable.getTime());

        HistoricVariableUpdate intVariable = (HistoricVariableUpdate) details.get(2);
        assertEquals("bVariable", intVariable.getVariableName());
        assertEquals(123, intVariable.getValue());
        assertEquals(processInstance.getId(), intVariable.getProcessInstanceId());
        assertEquals(updatedDate, intVariable.getTime());

        HistoricVariableUpdate longVariable = (HistoricVariableUpdate) details.get(3);
        assertEquals("cVariable", longVariable.getVariableName());
        assertEquals(12345L, longVariable.getValue());
        assertEquals(processInstance.getId(), longVariable.getProcessInstanceId());
        assertEquals(updatedDate, longVariable.getTime());

        HistoricVariableUpdate doubleVariable = (HistoricVariableUpdate) details.get(4);
        assertEquals("dVariable", doubleVariable.getVariableName());
        assertEquals(1234.567, doubleVariable.getValue());
        assertEquals(processInstance.getId(), doubleVariable.getProcessInstanceId());
        assertEquals(updatedDate, doubleVariable.getTime());

        HistoricVariableUpdate shortVariable = (HistoricVariableUpdate) details.get(5);
        assertEquals("eVariable", shortVariable.getVariableName());
        assertEquals((short) 12, shortVariable.getValue());
        assertEquals(processInstance.getId(), shortVariable.getProcessInstanceId());
        assertEquals(updatedDate, shortVariable.getTime());

        HistoricVariableUpdate dateVariable = (HistoricVariableUpdate) details.get(6);
        assertEquals("fVariable", dateVariable.getVariableName());
        assertEquals(sdf.parse("01/01/2001 01:23:45 678"), dateVariable.getValue());
        assertEquals(processInstance.getId(), dateVariable.getProcessInstanceId());
        assertEquals(updatedDate, dateVariable.getTime());

        HistoricVariableUpdate serializableVariable = (HistoricVariableUpdate) details.get(7);
        assertEquals("gVariable", serializableVariable.getVariableName());
        assertEquals(new SerializableVariable("hello hello"), serializableVariable.getValue());
        assertEquals(processInstance.getId(), serializableVariable.getProcessInstanceId());
        assertEquals(updatedDate, serializableVariable.getTime());

        HistoricVariableUpdate byteArrayVariable = (HistoricVariableUpdate) details.get(8);
        assertEquals("hVariable", byteArrayVariable.getVariableName());
        assertEquals(";-)", new String((byte[]) byteArrayVariable.getValue()));
        assertEquals(processInstance.getId(), byteArrayVariable.getProcessInstanceId());
        assertEquals(updatedDate, byteArrayVariable.getTime());

        // end process instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

        // check for historic process variables set
        HistoricVariableInstanceQuery historicProcessVariableQuery = historyService
                .createHistoricVariableInstanceQuery()
                .orderByVariableName().asc();

        assertEquals(8, historicProcessVariableQuery.count());

        List<HistoricVariableInstance> historicVariables = historicProcessVariableQuery.list();

        // Variable status when process is finished
        HistoricVariableInstance historicVariable = historicVariables.get(0);
        assertEquals("aVariable", historicVariable.getVariableName());
        assertEquals("updated value", historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(1);
        assertEquals("bVariable", historicVariable.getVariableName());
        assertEquals(123, historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(2);
        assertEquals("cVariable", historicVariable.getVariableName());
        assertEquals(12345L, historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(3);
        assertEquals("dVariable", historicVariable.getVariableName());
        assertEquals(1234.567, historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(4);
        assertEquals("eVariable", historicVariable.getVariableName());
        assertEquals((short) 12, historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(5);
        assertEquals("fVariable", historicVariable.getVariableName());
        assertEquals(sdf.parse("01/01/2001 01:23:45 678"), historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(6);
        assertEquals("gVariable", historicVariable.getVariableName());
        assertEquals(new SerializableVariable("hello hello"), historicVariable.getValue());
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        historicVariable = historicVariables.get(7);
        assertEquals("hVariable", historicVariable.getVariableName());
        assertEquals(";-)", ";-)", new String((byte[]) historicVariable.getValue()));
        assertEquals(processInstance.getId(), historicVariable.getProcessInstanceId());

        processEngineConfiguration.resetClock();
    }

}
