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
package org.flowable.engine.test.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ExclusiveGatewayTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testDivergingExclusiveGateway() {
        for (int i = 1; i <= 3; i++) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("exclusiveGwDiverging", CollectionUtil.singletonMap("input", i));
            assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task " + i);
            runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
        }
    }

    @Test
    @Deployment
    public void testSkipExpression() {
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
            variables.put("input", -i);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("exclusiveGwDivergingSkipExpression", variables);
            assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task " + i);
            runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
        }
    }

    @Test
    @Deployment
    public void testMergingExclusiveGateway() {
        runtimeService.startProcessInstanceByKey("exclusiveGwMerging");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
    }

    // If there are multiple outgoing seqFlow with valid conditions, the first
    // defined one should be chosen.
    @Test
    @Deployment
    public void testMultipleValidConditions() {
        runtimeService.startProcessInstanceByKey("exclusiveGwMultipleValidConditions", CollectionUtil.singletonMap("input", 5));
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task 2");
    }

    @Test
    @Deployment
    public void testNoSequenceFlowSelected() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exclusiveGwNoSeqFlowSelected", CollectionUtil.singletonMap("input", 4)))
                .isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("No outgoing sequence flow of the exclusive gateway " + "'exclusiveGw' could be selected for continuing Execution[ id")
                .hasMessageContainingAll(" - definition 'exclusiveGwNoSeqFlowSelected:1:", " - activity 'exclusiveGw'");
    }

    /**
     * Test for bug ACT-10: whitespaces/newlines in expressions lead to exceptions
     */
    @Test
    @Deployment
    public void testWhitespaceInExpression() {
        // Starting a process instance will lead to an exception if whitespace
        // are incorrectly handled
        runtimeService.startProcessInstanceByKey("whiteSpaceInExpression", CollectionUtil.singletonMap("input", 1));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/ExclusiveGatewayTest.testDivergingExclusiveGateway.bpmn20.xml" })
    public void testUnknownVariableInExpression() {
        // Instead of 'input' we're starting a process instance with the name
        // 'iinput' (ie. a typo)
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exclusiveGwDiverging", CollectionUtil.singletonMap("iinput", 1)))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Unknown property used in expression");
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanProperty() {
        runtimeService.startProcessInstanceByKey("decisionBasedOnBeanProperty", CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(150)));

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Standard service");
    }

    @Test
    @Deployment
    public void testDecideBasedOnListOrArrayOfBeans() {
        List<ExclusiveGatewayTestOrder> orders = new ArrayList<>();
        orders.add(new ExclusiveGatewayTestOrder(50));
        orders.add(new ExclusiveGatewayTestOrder(300));
        orders.add(new ExclusiveGatewayTestOrder(175));

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("decisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Gold Member service");

        // Arrays are usable in exactly the same way
        ExclusiveGatewayTestOrder[] orderArray = orders.toArray(new ExclusiveGatewayTestOrder[orders.size()]);
        orderArray[1].setPrice(10);
        pi = runtimeService.startProcessInstanceByKey("decisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orderArray));

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Basic service");
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanMethod() {
        runtimeService.startProcessInstanceByKey("decisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(300)));

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Gold Member service");
    }

    @Test
    @Deployment
    public void testInvalidMethodExpression() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidMethodExpression", CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(50))))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Unknown method used in expression");
    }

    @Test
    @Deployment
    public void testDefaultSequenceFlow() {

        // Input == 1 -> default is not selected
        String procId = runtimeService.startProcessInstanceByKey("exclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 1)).getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Input is one");
        runtimeService.deleteProcessInstance(procId, null);

        runtimeService.startProcessInstanceByKey("exclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 5)).getId();
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Default input");
    }

    @Test
    public void testInvalidProcessDefinition() {
        String defaultFlowWithCondition = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>"
                + "  <process id='exclusiveGwDefaultSequenceFlow'> " + "    <startEvent id='theStart' /> " + "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> " +

                "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' default='flow3' /> " + "    <sequenceFlow id='flow2' sourceRef='exclusiveGw' targetRef='theTask1'> "
                + "      <conditionExpression xsi:type='tFormalExpression'>${input == 1}</conditionExpression> " + "    </sequenceFlow> "
                + "    <sequenceFlow id='flow3' sourceRef='exclusiveGw' targetRef='theTask2'> " + "      <conditionExpression xsi:type='tFormalExpression'>${input == 3}</conditionExpression> "
                + "    </sequenceFlow> " +

                "    <userTask id='theTask1' name='Input is one' /> " + "    <userTask id='theTask2' name='Default input' /> " + "  </process>" + "</definitions>";

        assertThatThrownBy(() -> repositoryService.createDeployment().addString("myprocess.bpmn20.xml", defaultFlowWithCondition).deploy());

        String noOutgoingFlow = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>"
                + "  <process id='exclusiveGwDefaultSequenceFlow'> " + "    <startEvent id='theStart' /> " + "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> "
                + "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' /> " + "  </process>" + "</definitions>";
        assertThatThrownBy(() -> repositoryService.createDeployment().addString("myprocess.bpmn20.xml", noOutgoingFlow).deploy())
                .as("Could deploy a process definition with a XOR Gateway without outgoing sequence flows.")
                .isInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testAsyncExclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncExclusive", CollectionUtil.singletonMap("input", 1));

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        managementService.executeJob(job.getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Input is one");
    }

    // From https://github.com/Activiti/Activiti/issues/796
    @Test
    @Deployment
    public void testExclusiveDirectlyToEnd() {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("input", 1);
            ProcessInstance startProcessInstanceByKey = runtimeService.startProcessInstanceByKey("exclusiveGateway", variables);
            long count = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(startProcessInstanceByKey.getId()).unfinished()
                    .count();
            assertThat(count).isZero();
        }
    }

}
