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

package org.flowable.engine.test.transactions;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.ManagementService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @Author Joram Barrez
 */
public class TransactionRollbackTest extends PluggableFlowableTestCase {

    public static class Buzzz implements ActivityBehavior {

        private static final long serialVersionUID = 1L;

        @Override
        public void execute(DelegateExecution execution) {
            throw new FlowableException("Buzzz");
        }
    }

    public static class NestedCommandDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            try {
                ManagementService managementService = CommandContextUtil.getProcessEngineConfiguration().getManagementService();
                managementService.executeCommand((Command<Void>) commandContext -> { throw new RuntimeException("exception from service task"); });
            } catch (Exception e) {
                e.printStackTrace();
            }

            execution.setVariable("theVariable", "test");
        }

    }

    @Test
    @Deployment
    public void testRollback() {
        try {
            runtimeService.startProcessInstanceByKey("RollbackProcess");

            fail("Starting the process instance should throw an exception");

        } catch (Exception e) {
            assertEquals("Buzzz", e.getMessage());
        }

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/transactions/trivial.bpmn20.xml", "org/flowable/engine/test/transactions/rollbackAfterSubProcess.bpmn20.xml" })
    public void testRollbackAfterSubProcess() {
        try {
            runtimeService.startProcessInstanceByKey("RollbackAfterSubProcess");

            fail("Starting the process instance should throw an exception");

        } catch (Exception e) {
            assertEquals("Buzzz", e.getMessage());
        }

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Test
    @Deployment
    public void testNoRollbackInNestedCommand() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("testProcess").start();

        // The task should be created, as the service task with an exception is try-catched in the delegate.
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        String variable = (String) runtimeService.getVariable(processInstance.getId(), "theVariable");
        assertEquals("test", variable);
    }

}
