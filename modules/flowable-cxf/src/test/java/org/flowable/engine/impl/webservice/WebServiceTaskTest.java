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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertThat(webServiceMock.getCount()).isZero();
        assertThat(processInstance.isEnded()).isTrue();
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

        assertThat(webServiceMock.getDataStructure().eltDate).isEqualTo(expectedDate);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testFaultManagement() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        // Expected fault caught with a boundary error event

        webServiceMock.setTo(Integer.MAX_VALUE);
        ProcessInstance processInstanceWithExpectedFault = runtimeService
                .startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);
        assertThat(processInstanceWithExpectedFault.isEnded()).isTrue();
        final List<HistoricProcessInstance> historicProcessInstanceWithExpectedFault = historyService
                .createHistoricProcessInstanceQuery().processInstanceId(processInstanceWithExpectedFault.getId())
                .list();
        assertThat(historicProcessInstanceWithExpectedFault).hasSize(1);
        assertThat(historicProcessInstanceWithExpectedFault.get(0).getEndActivityId()).isEqualTo("theEndWithError");

        // Runtime exception occurring during processing of the web-service, so not caught in the process definition.
        // The runtime exception is embedded as an unexpected fault at web-service server side.
        webServiceMock.setTo(123456);
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("webServiceInvocation"))
                .isInstanceOf(FlowableException.class)
                .hasCauseInstanceOf(SoapFault.class)
                // Exception processed as Business fault is false
                .isNotInstanceOf(BpmnError.class);

        // Unexpected fault at ws-client side invoking the web-service, so not caught in the process definition
        server.stop();
        try {
            assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("webServiceInvocation"))
                    .isInstanceOf(FlowableException.class)
                    .hasCauseInstanceOf(Fault.class)
                    // Exception processed as Business fault is false
                    .isNotInstanceOf(BpmnError.class);
        } finally {
            server.start();
        }
    }

    @Test
    @Deployment
    public void testFaultManagementREatWSClientLevel() throws Exception {

        // Runtime exception occurring during processing of the web-service at ws-client side, so not caught in the
        // process definition. A runtime exception is generated by CXF client when encountering an unknown URL scheme.
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("webServiceInvocationRuntimeExceptionAtWsClientLevel"))
                .isInstanceOf(FlowableException.class)
                // Exception processed as Business fault is false
                .isNotInstanceOf(BpmnError.class);
    }

    @Test
    @Deployment
    public void testWebServiceInvocationWithEndpointAddressConfigured() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("webServiceInvocation");
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        assertThat(webServiceMock.getCount()).isZero();
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonArrayVariableMultiInstanceLoop() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ArrayNode values = mapper.createArrayNode();
        values.add(21);
        values.add(32);
        values.add(43);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("values", values);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonArrayVariableMultiInstanceLoop",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 21 + 32 + 43 = 95
        assertThat(webServiceMock.getCount()).isEqualTo(95);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonArrayVariableDirectInvocation() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ArrayNode values = mapper.createArrayNode();
        values.add(1);
        values.add(2);
        values.add(3);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("values", values);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonArrayVariableDirectInvocation",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 1 + 2 + 3 = 5
        assertThat(webServiceMock.getCount()).isEqualTo(5);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonBeanWithArrayVariableMultiInstanceLoop() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        final ArrayNode values = valuesObj.putArray("values");
        values.add(12);
        values.add(23);
        values.add(34);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanWithArrayVariableMultiInstanceLoop", initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 12 + 23 + 34 = 68
        assertThat(webServiceMock.getCount()).isEqualTo(68);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonBeanWithArrayVariableDirectInvocation() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        final ArrayNode values = valuesObj.putArray("values");
        values.add(11);
        values.add(22);
        values.add(33);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanWithArrayVariableDirectInvocation", initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 11 + 22 + 33 = 65
        assertThat(webServiceMock.getCount()).isEqualTo(65);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonBeanVariableInvocationByAttribute() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode valuesObj = mapper.createObjectNode();
        valuesObj.put("value1", 111);
        valuesObj.put("value2", 222);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("bean", valuesObj);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("jsonBeanVariableInvocationByAttribute",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 111 + 222 = 332
        assertThat(webServiceMock.getCount()).isEqualTo(332);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testJsonBeanVariableDirectInvocation() throws Exception {

        assertThat(webServiceMock.getCount()).isEqualTo(-1);

        processEngineConfiguration.addWsEndpointAddress(
                new QName("http://webservice.impl.engine.flowable.org/", "CounterImplPort"),
                new URL(WEBSERVICE_MOCK_ADDRESS));

        final ObjectMapper mapper = this.processEngineConfiguration.getObjectMapper();
        final ObjectNode argsObj = mapper.createObjectNode();
        final ObjectNode valuesObj = mapper.createObjectNode();
        valuesObj.put("arg1", 1111);
        valuesObj.put("arg2", 2222);
        argsObj.set("args", valuesObj);

        final Map<String, Object> initVariables = new HashMap<>();
        initVariables.put("bean", argsObj);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("jsonBeanVariableDirectInvocation",
                initVariables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        // -1 (initial value of counter) + 1111 + 2222 = 3332
        assertThat(webServiceMock.getCount()).isEqualTo(3332);
        assertThat(processInstance.isEnded()).isTrue();
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

        assertThat(processInstance.isEnded()).isTrue();

        final HistoricProcessInstance histProcInst = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).includeProcessVariables().singleResult();
        final Object currentStructure = histProcInst.getProcessVariables().get("currentStructure");
        assertThat(currentStructure).isInstanceOf(JsonNode.class);
        final JsonNode currentStructureJson = (JsonNode) currentStructure;
        assertThat(currentStructureJson.findValue("eltString").asText()).isEqualTo(myString);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        final String myDateJson = currentStructureJson.findValue("eltDate").asText();
        assertThat(sdf.parse(myDateJson)).isEqualTo(myDate);
    }

    /**
     * Unit test relative to issue <a href="https://github.com/flowable/flowable-engine/issues/2871">2871</a>.
     */
    @Test
    @Deployment
    public void testWebServiceInvocationReturningSeveralParams() throws Exception {

        final String inParam = "23";
        final Map<String, Object> variables = new HashMap<>(1);
        variables.put("inParam", inParam);
        final ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("webServiceInvocationReturningSeveralParams", variables);
        waitForJobExecutorToProcessAllJobs(10000L, 250L);

        final HistoricProcessInstance histProcInst = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).includeProcessVariables().singleResult();
        final Map<String, Object> procVariables = histProcInst.getProcessVariables();
        assertEquals("23", procVariables.get("outParam1"));
        assertEquals(23, procVariables.get("outParam2"));
        assertEquals("23-23", procVariables.get("outParam3"));
    }

}
