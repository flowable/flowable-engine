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

package org.flowable.engine.test.bpmn.subprocess;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.interceptor.StartProcessInstanceAfterContext;
import org.flowable.engine.interceptor.StartProcessInstanceBeforeContext;
import org.flowable.engine.interceptor.StartProcessInstanceInterceptor;
import org.flowable.engine.interceptor.StartSubProcessInstanceAfterContext;
import org.flowable.engine.interceptor.StartSubProcessInstanceBeforeContext;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CallActivityTest extends ResourceFlowableTestCase {

    private static final String MAIN_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSuspendedProcessCallActivity_mainProcess.bpmn.xml";
    private static final String CHILD_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSuspendedProcessCallActivity_childProcess.bpmn.xml";
    private static final String MESSAGE_TRIGGERED_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSuspendedProcessCallActivity_messageTriggeredProcess.bpmn.xml";
    private static final String INHERIT_VARIABLES_MAIN_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testInheritVariablesCallActivity_mainProcess.bpmn20.xml";
    private static final String INHERIT_VARIABLES_CHILD_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSameDeploymentCallActivity_childProcess.bpmn20.xml";
    private static final String SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSameDeploymentCallActivity_mainProcess.bpmn20.xml";
    private static final String SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSameDeploymentCallActivity_childProcess.bpmn20.xml";
    private static final String SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testSameDeploymentCallActivity_childProcess_v2.bpmn20.xml";
    private static final String NOT_SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testNotSameDeploymentCallActivity_mainProcess.bpmn20.xml";
    private static final String NOT_INHERIT_VARIABLES_MAIN_PROCESS_RESOURCE = "org/flowable/engine/test/bpmn/subprocess/SubProcessTest.testNotInheritVariablesCallActivity_mainProcess.bpmn20.xml";

    public CallActivityTest() {
        super("org/flowable/standalone/parsing/encoding.flowable.cfg.xml");
    }

    @Test
    public void testInstantiateProcessByMessage() throws Exception {
        BpmnModel messageTriggeredBpmnModel = loadBPMNModel(MESSAGE_TRIGGERED_PROCESS_RESOURCE);

        processEngine.getRepositoryService().createDeployment().name("messageTriggeredProcessDeployment")
                .addBpmnModel("messageTriggered.bpmn20.xml", messageTriggeredBpmnModel).deploy();

        ProcessInstance childProcessInstance = runtimeService.startProcessInstanceByMessage("TRIGGER_PROCESS_MESSAGE");
        assertNotNull(childProcessInstance);
    }

    @Test
    public void testInstantiateSuspendedProcessByMessage() throws Exception {
        BpmnModel messageTriggeredBpmnModel = loadBPMNModel(MESSAGE_TRIGGERED_PROCESS_RESOURCE);

        Deployment messageTriggeredBpmnDeployment = processEngine.getRepositoryService().createDeployment().name("messageTriggeredProcessDeployment")
                .addBpmnModel("messageTriggered.bpmn20.xml", messageTriggeredBpmnModel).deploy();

        suspendProcessDefinitions(messageTriggeredBpmnDeployment);

        try {
            runtimeService.startProcessInstanceByMessage("TRIGGER_PROCESS_MESSAGE");
            fail("Exception expected");
        } catch (FlowableException ae) {
            assertTextPresent("Cannot start process instance. Process definition Message Triggered Process", ae.getMessage());
        }

    }

    @Test
    public void testInstantiateChildProcess() throws Exception {
        BpmnModel childBpmnModel = loadBPMNModel(CHILD_PROCESS_RESOURCE);

        processEngine.getRepositoryService().createDeployment().name("childProcessDeployment").addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        ProcessInstance childProcessInstance = runtimeService.startProcessInstanceByKey("childProcess");
        assertNotNull(childProcessInstance);
    }

    @Test
    public void testInstantiateSuspendedChildProcess() throws Exception {
        BpmnModel childBpmnModel = loadBPMNModel(CHILD_PROCESS_RESOURCE);

        Deployment childDeployment = processEngine.getRepositoryService().createDeployment().name("childProcessDeployment").addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        suspendProcessDefinitions(childDeployment);

        try {
            runtimeService.startProcessInstanceByKey("childProcess");
            fail("Exception expected");
        } catch (FlowableException ae) {
            assertTextPresent("Cannot start process instance. Process definition Child Process", ae.getMessage());
        }

    }

    @Test
    public void testInstantiateSubprocess() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(CHILD_PROCESS_RESOURCE);

        Deployment childDeployment = processEngine.getRepositoryService().createDeployment().name("childProcessDeployment").addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        processEngine.getRepositoryService().createDeployment().name("masterProcessDeployment").addBpmnModel("masterProcess.bpmn20.xml", mainBpmnModel).deploy();

        suspendProcessDefinitions(childDeployment);

        try {
            runtimeService.startProcessInstanceByKey("masterProcess");
            fail("Exception expected");
        } catch (FlowableException ae) {
            assertTextPresent("Cannot start process instance. Process definition Child Process", ae.getMessage());
        }

    }

    @Test
    public void testInheritVariablesSubprocess() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(INHERIT_VARIABLES_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(INHERIT_VARIABLES_CHILD_PROCESS_RESOURCE);

        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel).deploy();

        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "String test value");
        variables.put("var2", true);
        variables.put("var3", 12345);
        variables.put("var4", 67890);

        ProcessInstance mainProcessInstance = runtimeService.startProcessInstanceByKey("mainProcess", variables);

        HistoricActivityInstanceQuery activityInstanceQuery = historyService.createHistoricActivityInstanceQuery();
        activityInstanceQuery.processInstanceId(mainProcessInstance.getId());
        activityInstanceQuery.activityId("childProcessCall");
        HistoricActivityInstance activityInstance = activityInstanceQuery.singleResult();
        String calledInstanceId = activityInstance.getCalledProcessInstanceId();

        HistoricVariableInstanceQuery variableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
        List<HistoricVariableInstance> variableInstances = variableInstanceQuery.processInstanceId(calledInstanceId).list();

        assertEquals(4, variableInstances.size());
        for (HistoricVariableInstance variable : variableInstances) {
            assertEquals(variables.get(variable.getVariableName()), variable.getValue());
        }
    }

    @Test
    public void testNotInheritVariablesSubprocess() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(NOT_INHERIT_VARIABLES_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(INHERIT_VARIABLES_CHILD_PROCESS_RESOURCE);

        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel).deploy();

        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "String test value");
        variables.put("var2", true);
        variables.put("var3", 12345);
        variables.put("var4", 67890);

        ProcessInstance mainProcessInstance = runtimeService.startProcessInstanceByKey("mainProcess", variables);

        HistoricActivityInstanceQuery activityInstanceQuery = historyService.createHistoricActivityInstanceQuery();
        activityInstanceQuery.processInstanceId(mainProcessInstance.getId());
        activityInstanceQuery.activityId("childProcessCall");
        HistoricActivityInstance activityInstance = activityInstanceQuery.singleResult();
        String calledInstanceId = activityInstance.getCalledProcessInstanceId();

        HistoricVariableInstanceQuery variableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
        variableInstanceQuery.processInstanceId(calledInstanceId);
        List<HistoricVariableInstance> variableInstances = variableInstanceQuery.list();

        assertEquals(0, variableInstances.size());
    }

    @Test
    public void testSameDeploymentSubprocess() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel)
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKey("mainProcess");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the child process within the same deployment", "User Task", task.getName());
    }
    
    @Test
    public void testSameDeploymentSubprocessWithTenant() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel)
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKeyAndTenantId("mainProcess", "myTenant");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the child process within the same deployment", "User Task", task.getName());
    }

    @Test
    public void testNotSameDeploymentSubprocess() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(NOT_SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel)
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKey("mainProcess");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the newest child process deployment", "User Task V2", task.getName());
    }
    
    @Test
    public void testNotSameDeploymentSubprocessWithTenant() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(NOT_SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel)
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKeyAndTenantId("mainProcess", "myTenant");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the newest child process deployment", "User Task V2", task.getName());
    }

    @Test
    public void testSameDeploymentSubprocessNotInSameDeployment() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel).deploy();

        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKey("mainProcess");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the newest child process deployment as it there " +
                "is no deployed child process in the same deployment", "User Task V2", task.getName());
    }
    
    @Test
    public void testSameDeploymentSubprocessNotInSameDeploymentWithTenant() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_PROCESS_RESOURCE);
        BpmnModel childV2BpmnModel = loadBPMNModel(SAME_DEPLOYMENT_CHILD_V2_PROCESS_RESOURCE);

        // deploy the main and child process within one deployment
        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel).deploy();

        processEngine.getRepositoryService()
                .createDeployment()
                .name("mainProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();

        // deploy a new version of the child process in which the user task has an updated name
        processEngine.getRepositoryService()
                .createDeployment()
                .name("childProcessDeployment")
                .tenantId("myTenant")
                .addBpmnModel("childProcessV2.bpmn20.xml", childV2BpmnModel).deploy();

        runtimeService.startProcessInstanceByKeyAndTenantId("mainProcess", "myTenant");

        List<Task> list = taskService.createTaskQuery().list();
        assertEquals("There must be one task from the child process", 1, list.size());

        Task task = list.get(0);
        assertEquals("The child process must have the name of the newest child process deployment as it there " +
                "is no deployed child process in the same deployment", "User Task V2", task.getName());
    }
    
    @Test
    public void testStartSubProcessInstanceInterceptor() throws Exception {
        BpmnModel mainBpmnModel = loadBPMNModel(NOT_INHERIT_VARIABLES_MAIN_PROCESS_RESOURCE);
        BpmnModel childBpmnModel = loadBPMNModel(INHERIT_VARIABLES_CHILD_PROCESS_RESOURCE);
        
        TestStartProcessInstanceInterceptor testStartProcessInstanceInterceptor = new TestStartProcessInstanceInterceptor();
        processEngineConfiguration.setStartProcessInstanceInterceptor(testStartProcessInstanceInterceptor);
        
        try {
            processEngine.getRepositoryService()
                    .createDeployment()
                    .name("mainProcessDeployment")
                    .addBpmnModel("mainProcess.bpmn20.xml", mainBpmnModel).deploy();
    
            processEngine.getRepositoryService()
                    .createDeployment()
                    .name("childProcessDeployment")
                    .addBpmnModel("childProcess.bpmn20.xml", childBpmnModel).deploy();
    
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "test value");
    
            ProcessInstance mainProcessInstance = runtimeService.startProcessInstanceByKey("mainProcess", variables);
            assertEquals("testKey", mainProcessInstance.getBusinessKey());
            
            HistoricProcessInstance subProcessInstance = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(mainProcessInstance.getId()).singleResult();
            assertEquals("testSubKey", subProcessInstance.getBusinessKey());
            
            HistoricVariableInstanceQuery variableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
            List<HistoricVariableInstance> variableInstances = variableInstanceQuery.processInstanceId(mainProcessInstance.getId()).list();
    
            assertEquals(2, variableInstances.size());
            Map<String, Object> variableMap = new HashMap<>();
            for (HistoricVariableInstance variable : variableInstances) {
                variableMap.put(variable.getVariableName(), variable.getValue());
            }
            assertEquals("test value", variableMap.get("var1"));
            assertEquals("test", variableMap.get("beforeContextVar"));
            
            variableInstances = variableInstanceQuery.processInstanceId(subProcessInstance.getId()).list();
            
            assertEquals(3, variableInstances.size());
            variableMap = new HashMap<>();
            for (HistoricVariableInstance variable : variableInstances) {
                variableMap.put(variable.getVariableName(), variable.getValue());
            }
            assertEquals("test value", variableMap.get("var1"));
            assertEquals("test", variableMap.get("beforeContextVar"));
            assertEquals("subtest", variableMap.get("beforeSubContextVar"));
            
        } finally {
            processEngineConfiguration.setStartProcessInstanceInterceptor(null);
        }
    }

    private void suspendProcessDefinitions(Deployment childDeployment) {
        List<ProcessDefinition> childProcessDefinitionList = repositoryService.createProcessDefinitionQuery().deploymentId(childDeployment.getId()).list();

        for (ProcessDefinition processDefinition : childProcessDefinitionList) {
            repositoryService.suspendProcessDefinitionById(processDefinition.getId());
        }
    }

    @AfterEach
    protected void tearDown() throws Exception {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected BpmnModel loadBPMNModel(String bpmnModelFilePath) throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(bpmnModelFilePath);
        StreamSource xmlSource = new InputStreamSource(xmlStream);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xmlSource, false, false, processEngineConfiguration.getXmlEncoding());
        return bpmnModel;
    }
    
    protected class TestStartProcessInstanceInterceptor implements StartProcessInstanceInterceptor {
        
        protected int beforeStartProcessInstanceCounter = 0;
        protected int afterStartProcessInstanceCounter = 0;
        protected int beforeStartSubProcessInstanceCounter = 0;
        protected int afterStartSubProcessInstanceCounter = 0;

        @Override
        public void beforeStartProcessInstance(StartProcessInstanceBeforeContext instanceContext) {
            beforeStartProcessInstanceCounter++;
            instanceContext.getVariables().put("beforeContextVar", "test");
            instanceContext.setBusinessKey("testKey");
        }

        @Override
        public void afterStartProcessInstance(StartProcessInstanceAfterContext instanceContext) {
            afterStartProcessInstanceCounter++;
        }

        @Override
        public void beforeStartSubProcessInstance(StartSubProcessInstanceBeforeContext instanceContext) {
            beforeStartSubProcessInstanceCounter++;
            instanceContext.getVariables().put("beforeSubContextVar", "subtest");
            instanceContext.setBusinessKey("testSubKey");
            instanceContext.setInheritVariables(true);
        }

        @Override
        public void afterStartSubProcessInstance(StartSubProcessInstanceAfterContext instanceContext) {
            afterStartSubProcessInstanceCounter++;
        }

        public int getBeforeStartProcessInstanceCounter() {
            return beforeStartProcessInstanceCounter;
        }

        public int getAfterStartProcessInstanceCounter() {
            return afterStartProcessInstanceCounter;
        }

        public int getBeforeStartSubProcessInstanceCounter() {
            return beforeStartSubProcessInstanceCounter;
        }

        public int getAfterStartSubProcessInstanceCounter() {
            return afterStartSubProcessInstanceCounter;
        }
    }

}
