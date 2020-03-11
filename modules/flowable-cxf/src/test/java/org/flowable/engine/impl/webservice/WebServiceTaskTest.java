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
package org.flowable.engine.impl.webservice;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An integration test for CXF based web services
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class WebServiceTaskTest extends AbstractWebServiceTaskTest {

    @Test
    @Deployment
    public void testWebServiceInvocation() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertEquals(0, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testWebServiceInvocationDataStructure() throws Exception {

        final Calendar calendar = Calendar.getInstance();
        calendar.set(2015, Calendar.APRIL, 23, 0, 0, 0);
        final Date expectedDate = calendar.getTime();
        final Map<String, Object> variables = new HashMap<>(1);
        variables.put("startDate", expectedDate);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("webServiceInvocationDataStructure", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertEquals(expectedDate, webServiceMock.getDataStructure().eltDate);
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testFaultManagement() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        // Expected fault caught with a boundary error event

        webServiceMock.setTo(Integer.MAX_VALUE);
        ProcessInstance processInstanceWithExpectedFault = runtimeService
                .startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);
        assertTrue(processInstanceWithExpectedFault.isEnded());
        final List<HistoricProcessInstance> historicProcessInstanceWithExpectedFault = historyService
                .createHistoricProcessInstanceQuery().processInstanceId(processInstanceWithExpectedFault.getId())
                .list();
        assertEquals(1, historicProcessInstanceWithExpectedFault.size());
        assertEquals("theEndWithError", historicProcessInstanceWithExpectedFault.get(0).getEndActivityId());

        // Runtime exception occurring during processing of the web-service, so not caught in the process definition.
        // The runtime exception is embedded as an unexpected fault at web-service server side.
        webServiceMock.setTo(123456);
        try {
            runtimeService.startProcessInstanceByKey("webServiceInvocation");
        } catch (FlowableException e) {
            assertFalse("Exception processed as Business fault", e instanceof BpmnError);
            assertTrue(e.getCause() instanceof SoapFault);
        }

        // Unexpected fault at ws-client side invoking the web-service, so not caught in the process definition
        server.stop();
        try {
            runtimeService.startProcessInstanceByKey("webServiceInvocation");
        } catch (FlowableException e) {
            assertFalse("Exception processed as Business fault", e instanceof BpmnError);
            assertTrue(e.getCause() instanceof Fault);
        } finally {
            server.start();
        }
    }

    @Test
    @Deployment
    public void testFaultManagementREatWSClientLevel() throws Exception {

        // Runtime exception occurring during processing of the web-service at ws-client side, so not catched in the
        // process definition. A runtime exception is generated by CXF client when encountering an unknown URL scheme.
        try {
            runtimeService.startProcessInstanceByKey("webServiceInvocationRuntimeExceptionAtWsClientLevel");
        } catch (FlowableException e) {
            assertFalse("Exception '" + e.getClass().getName() + "' processed as Business fault",
                    e instanceof BpmnError);
        }
    }

    @Test
    @Deployment
    public void testWebServiceInvocationWithEndpointAddressConfigured() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertEquals(0, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonArrayVariableMultiInstanceLoop() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ArrayNode values = mapper.createArrayNode();
        values.add(21);
        values.add(32);
        values.add(43);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("values", values);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonArrayVariableMultiInstanceLoop",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 21 + 32 + 43 = 95
        assertEquals(95, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonArrayVariableDirectInvocation() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ArrayNode values = mapper.createArrayNode();
        values.add(1);
        values.add(2);
        values.add(3);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("values", values);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonArrayVariableDirectInvocation",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 1 + 2 + 3 = 5
        assertEquals(5, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonBeanWithArrayVariableMultiInstanceLoop() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        final ArrayNode values = valuesObj.putArray("values");
        values.add(12);
        values.add(23);
        values.add(34);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanWithArrayVariableMultiInstanceLoop", initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 12 + 23 + 34 = 68
        assertEquals(68, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonBeanWithArrayVariableDirectInvocation() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        final ArrayNode values = valuesObj.putArray("values");
        values.add(11);
        values.add(22);
        values.add(33);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanWithArrayVariableDirectInvocation", initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 11 + 22 + 33 = 65
        assertEquals(65, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonBeanVariableInvocationByAttribute() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        valuesObj.put("value1", 111);
        valuesObj.put("value2", 222);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanVariableInvocationByAttribute",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 111 + 222 = 332
        assertEquals(332, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testJsonBeanVariableDirectInvocation() throws Exception {

        assertEquals(-1, webServiceMock.getCount());

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode argsObj = mapper.createObjectNode();
        final ObjectNode valuesObj = mapper.createObjectNode();
        valuesObj.put("arg1", 1111);
        valuesObj.put("arg2", 2222);
        argsObj.set("args", valuesObj);

        final Map<String, Object> initVariables = new HashMap<String, Object>();
        initVariables.put("bean", argsObj);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonBeanVariableDirectInvocation",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 1111 + 2222 = 3332
        assertEquals(3332, webServiceMock.getCount());
        assertTrue(processInstance.isEnded());
    }

    /**
     * Check the JSON conversion of a SOAP response XML part into JSon variable
     */
    @Test
    @Deployment
    public void testJsonDataObject() throws Exception {

        final String myString = "my-string";
        final GregorianCalendar myCalendar = new GregorianCalendar(2020, 1, 20, 0, 0, 0);
        final Date myDate = myCalendar.getTime();
        this.webServiceMock.setDataStructure(myString, myDate);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonDataObject");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertTrue(processInstance.isEnded());

        final HistoricProcessInstance histProcInst = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).includeProcessVariables().singleResult();
        final Object currentStructure = histProcInst.getProcessVariables().get("currentStructure");
        assertTrue(currentStructure instanceof JsonNode);
        final JsonNode currentStructureJson = (JsonNode) currentStructure;
        assertEquals(myString, currentStructureJson.findValue("eltString").asText());
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final String myDateJson = currentStructureJson.findValue("eltDate").asText();
        assertEquals(myDate, sdf.parse(myDateJson));
    }

}
