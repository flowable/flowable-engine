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
package org.flowable.examples.bpmn.usertask;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FinancialReportProcessTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        identityService.saveUser(identityService.newUser("fozzie"));
        identityService.saveUser(identityService.newUser("kermit"));

        identityService.saveGroup(identityService.newGroup("accountancy"));
        identityService.saveGroup(identityService.newGroup("management"));

        identityService.createMembership("fozzie", "accountancy");
        identityService.createMembership("kermit", "management");
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteUser("fozzie");
        identityService.deleteUser("kermit");
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/usertask/FinancialReportProcess.bpmn20.xml" })
    public void testProcess() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("financialReport");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertEquals(1, tasks.size());
        org.flowable.task.api.Task task = tasks.get(0);
        assertEquals("Write monthly financial report", task.getName());

        taskService.claim(task.getId(), "fozzie");
        tasks = taskService.createTaskQuery().taskAssignee("fozzie").list();

        assertEquals(1, tasks.size());
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertEquals(0, tasks.size());
        tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertEquals(1, tasks.size());
        assertEquals("Verify monthly financial report", tasks.get(0).getName());
        taskService.complete(tasks.get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

}
