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
package org.flowable.spring.test.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Frederik Heremans
 */
@ContextConfiguration(locations = "JPASpringTest-context.xml")
public class JpaTest extends SpringFlowableTestCase {

    @Test
    public void testJpaVariableHappyPath() {
        before();
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "John Doe");
        variables.put("amount", 15000L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("LoanRequestProcess", variables);

        // Variable should be present containing the loanRequest created by the
        // spring bean
        Object value = runtimeService.getVariable(processInstance.getId(), "loanRequest");
        assertThat(value).isInstanceOf(LoanRequest.class);
        LoanRequest request = (LoanRequest) value;
        assertThat(request.getCustomerName()).isEqualTo("John Doe");
        assertThat(request.getAmount().longValue()).isEqualTo(15000L);
        assertThat(request.isApproved()).isFalse();

        // We will approve the request, which will update the entity
        variables = new HashMap<>();
        variables.put("approvedByManager", Boolean.TRUE);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId(), variables);

        // If approved, the processInstance should be finished, gateway based
        // on loanRequest.approved value
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // Cleanup
        deleteDeployments();
    }

    @Test
    public void testJpaVariableDisapprovalPath() {

        before();
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Jane Doe");
        variables.put("amount", 50000);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("LoanRequestProcess", variables);

        // Variable should be present containing the loanRequest created by the
        // spring bean
        Object value = runtimeService.getVariable(processInstance.getId(), "loanRequest");
        assertThat(value).isInstanceOf(LoanRequest.class);
        LoanRequest request = (LoanRequest) value;
        assertThat(request.getCustomerName()).isEqualTo("Jane Doe");
        assertThat(request.getAmount().longValue()).isEqualTo(50000L);
        assertThat(request.isApproved()).isFalse();

        // We will disapprove the request, which will update the entity
        variables = new HashMap<>();
        variables.put("approvedByManager", Boolean.FALSE);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId(), variables);

        runtimeService.getVariable(processInstance.getId(), "loanRequest");
        request = (LoanRequest) value;
        assertThat(request.isApproved()).isFalse();

        // If disapproved, an extra task will be available instead of the
        // process ending
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Send rejection letter");

        // Cleanup
        deleteDeployments();
    }

    protected void before() {
        String[] defs = { "org/flowable/spring/test/jpa/JPASpringTest.bpmn20.xml" };
        for (String pd : defs)
            repositoryService.createDeployment().addClasspathResource(pd).deploy();
    }

    @Override
    protected void deleteDeployments() {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}
